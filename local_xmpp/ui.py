"""Minimal mediator UI over HTTP for local message injection and logs."""

from __future__ import annotations

import asyncio
import json
from dataclasses import asdict
from typing import List

from .stream import InboundMessage, LocalXmppServer


class MediatorUi:
    def __init__(self, xmpp_server: LocalXmppServer, host: str = "127.0.0.1", port: int = 8080) -> None:
        self.xmpp_server = xmpp_server
        self.host = host
        self.port = port
        self.logs: List[InboundMessage] = []
        self._server: asyncio.base_events.Server | None = None

    async def start(self) -> None:
        self._server = await asyncio.start_server(self._handle_http, self.host, self.port)
        asyncio.create_task(self._consume_inbound())

    async def stop(self) -> None:
        if self._server:
            self._server.close()
            await self._server.wait_closed()

    async def _consume_inbound(self) -> None:
        while True:
            msg = await self.xmpp_server.message_queue.get()
            self.logs.append(msg)

    async def _handle_http(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter) -> None:
        header_bytes = await reader.readuntil(b"\r\n\r\n")
        header_text = header_bytes.decode("utf-8", errors="ignore")
        request_line = header_text.splitlines()[0] if header_text else ""

        content_length = 0
        for line in header_text.splitlines()[1:]:
            if line.lower().startswith("content-length:"):
                content_length = int(line.split(":", 1)[1].strip())
                break

        body = ""
        if content_length:
            body = (await reader.readexactly(content_length)).decode("utf-8", errors="ignore")

        if request_line.startswith("GET / "):
            await self._respond(writer, 200, "text/html", _HTML)
        elif request_line.startswith("GET /logs "):
            payload = json.dumps([asdict(m) for m in self.logs])
            await self._respond(writer, 200, "application/json", payload)
        elif request_line.startswith("POST /send "):
            parsed = json.loads(body or "{}")
            sender = parsed.get("peer", "peer@local.node")
            text = parsed.get("message", "")
            await self.xmpp_server.inject_message(sender, text)
            await self._respond(writer, 200, "application/json", '{"status":"ok"}')
        else:
            await self._respond(writer, 404, "text/plain", "not found")

        writer.close()
        await writer.wait_closed()

    async def _respond(self, writer: asyncio.StreamWriter, status: int, ctype: str, body: str) -> None:
        raw = body.encode("utf-8")
        writer.write(
            (
                f"HTTP/1.1 {status} OK\r\n"
                f"Content-Type: {ctype}; charset=utf-8\r\n"
                f"Content-Length: {len(raw)}\r\n"
                "Connection: close\r\n\r\n"
            ).encode("utf-8")
            + raw
        )
        await writer.drain()


_HTML = """<!doctype html>
<html>
<head><meta charset='utf-8'><title>Local XMPP Mediator</title></head>
<body>
  <h1>Local XMPP Mediator</h1>
  <label>Recipient / JID</label><br>
  <input id='peer' value='alice@local.node'><br><br>
  <label>Message body</label><br>
  <input id='msg' value='hello'><button id='send'>Send</button>
  <h2>Inbound Messages</h2>
  <pre id='logs'></pre>
<script>
async function refresh() {
  const res = await fetch('/logs');
  const logs = await res.json();
  document.getElementById('logs').textContent = logs.map(l => `[${l.from_jid} -> ${l.to_jid}] ${l.body}`).join('\n');
}
setInterval(refresh, 1000); refresh();
document.getElementById('send').onclick = async () => {
  await fetch('/send', {
    method: 'POST',
    body: JSON.stringify({peer: document.getElementById('peer').value, message: document.getElementById('msg').value})
  });
};
</script>
</body>
</html>
"""
