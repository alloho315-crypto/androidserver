"""Async XMPP stream handler implementing RFC-style local server behavior."""

from __future__ import annotations

import asyncio
import re
import uuid
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from typing import Awaitable, Callable, Optional

from . import stanzas


@dataclass
class InboundMessage:
    from_jid: str
    to_jid: str
    body: str


class XmppSession:
    def __init__(
        self,
        reader: asyncio.StreamReader,
        writer: asyncio.StreamWriter,
        domain: str,
        on_message: Callable[[InboundMessage], Awaitable[None]],
    ) -> None:
        self.reader = reader
        self.writer = writer
        self.domain = domain
        self.on_message = on_message
        self.stream_id = uuid.uuid4().hex
        self.authenticated_user = "localuser"
        self.bound_jid: Optional[str] = None
        self._send_lock = asyncio.Lock()

    async def run(self) -> None:
        await self._expect_stream_opening()
        await self._send(stanzas.stream_header(self.domain, self.stream_id))
        await self._send(stanzas.features_pre_auth())

        auth_el = await self._read_stanza_element("auth")
        mechanism = auth_el.attrib.get("mechanism", "")
        if mechanism != "PLAIN":
            raise ValueError(f"Unsupported SASL mechanism {mechanism}")
        parsed = stanzas.parse_plain_auth((auth_el.text or "").strip())
        self.authenticated_user = parsed.authcid or "localuser"
        await self._send(stanzas.sasl_success())

        await self._expect_stream_opening()
        await self._send(stanzas.stream_header(self.domain, self.stream_id))
        await self._send(stanzas.features_post_auth())

        bind_iq = await self._read_stanza_element("iq")
        self.bound_jid = self._handle_bind(bind_iq)
        await self._send(stanzas.iq_result_bind(bind_iq.attrib["id"], self.bound_jid))

        session_iq = await self._read_stanza_element("iq")
        await self._send(stanzas.iq_result_empty(session_iq.attrib["id"]))

        while not self.reader.at_eof():
            stanza = await self._read_stanza_element()
            if stanza.tag.endswith("message") and stanza.attrib.get("type", "chat") == "chat":
                body_el = stanza.find("body")
                inbound = InboundMessage(
                    from_jid=stanza.attrib.get("from", ""),
                    to_jid=stanza.attrib.get("to", ""),
                    body=(body_el.text if body_el is not None else "") or "",
                )
                await self.on_message(inbound)
            # iq/presence are ignored in phase 1 but parsing hook remains here.

    def _handle_bind(self, iq: ET.Element) -> str:
        iq_id = iq.attrib.get("id")
        if not iq_id:
            raise ValueError("bind iq missing id")

        bind = None
        for child in iq:
            if child.tag.endswith("bind"):
                bind = child
                break
        if bind is None:
            raise ValueError("expected bind element")

        resource = "mobile"
        for child in bind:
            if child.tag.endswith("resource") and child.text:
                resource = child.text.strip()
                break
        return f"{self.authenticated_user}@{self.domain}/{resource}"

    async def send_chat_to_client(self, from_jid: str, body: str) -> None:
        if not self.bound_jid:
            return
        msg = stanzas.message_chat(
            from_jid=from_jid,
            to_jid=self.bound_jid,
            body=body,
            msg_id=uuid.uuid4().hex,
        )
        await self._send(msg)

    async def _expect_stream_opening(self) -> None:
        opening = await self.reader.readuntil(b">")
        if b"stream:stream" not in opening:
            raise ValueError("expected stream:stream opening")

    async def _read_stanza_element(self, expected_local_name: Optional[str] = None) -> ET.Element:
        stanza_text = await self._read_complete_stanza()
        el = stanzas.parse_xml_element(stanza_text)
        local_name = re.sub(r"^\{.*\}", "", el.tag)
        if expected_local_name and local_name != expected_local_name:
            raise ValueError(f"expected {expected_local_name}, got {local_name}")
        return el

    async def _read_complete_stanza(self) -> str:
        buf = ""
        while True:
            chunk = await self.reader.read(4096)
            if not chunk:
                raise ConnectionError("connection closed")
            buf += chunk.decode("utf-8", errors="strict")
            try:
                el = ET.fromstring(buf)
                if el is not None:
                    return buf
            except ET.ParseError:
                continue

    async def _send(self, xml_fragment: str) -> None:
        async with self._send_lock:
            self.writer.write(xml_fragment.encode("utf-8"))
            await self.writer.drain()


class LocalXmppServer:
    def __init__(self, host: str, port: int, domain: str) -> None:
        self.host = host
        self.port = port
        self.domain = domain
        self.server: Optional[asyncio.base_events.Server] = None
        self.active_session: Optional[XmppSession] = None
        self.message_queue: asyncio.Queue[InboundMessage] = asyncio.Queue()

    async def start(self) -> None:
        self.server = await asyncio.start_server(self._on_client, self.host, self.port)

    async def stop(self) -> None:
        if self.server:
            self.server.close()
            await self.server.wait_closed()

    async def _on_client(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter) -> None:
        session = XmppSession(reader, writer, self.domain, self._enqueue_inbound)
        self.active_session = session
        try:
            await session.run()
        finally:
            self.active_session = None
            writer.close()
            await writer.wait_closed()

    async def _enqueue_inbound(self, message: InboundMessage) -> None:
        await self.message_queue.put(message)

    async def inject_message(self, sender_jid: str, body: str) -> None:
        if self.active_session:
            await self.active_session.send_chat_to_client(sender_jid, body)
