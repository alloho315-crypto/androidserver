"""XMPP stanza generation and parsing helpers.

This module centralizes XML creation so stream/session logic stays readable.
"""

from __future__ import annotations

import base64
import html
import xml.etree.ElementTree as ET
from dataclasses import dataclass


NS_CLIENT = "jabber:client"
NS_STREAM = "http://etherx.jabber.org/streams"
NS_SASL = "urn:ietf:params:xml:ns:xmpp-sasl"
NS_BIND = "urn:ietf:params:xml:ns:xmpp-bind"
NS_SESSION = "urn:ietf:params:xml:ns:xmpp-session"


@dataclass
class PlainAuth:
    authzid: str
    authcid: str
    password: str


def parse_plain_auth(payload_b64: str) -> PlainAuth:
    raw = base64.b64decode(payload_b64).decode("utf-8", errors="strict")
    authzid, authcid, password = raw.split("\x00")
    return PlainAuth(authzid=authzid, authcid=authcid, password=password)


def stream_header(to_domain: str, stream_id: str) -> str:
    return (
        "<?xml version='1.0'?>"
        f"<stream:stream from='{html.escape(to_domain)}' "
        "xmlns='jabber:client' "
        "xmlns:stream='http://etherx.jabber.org/streams' "
        "version='1.0' "
        f"id='{html.escape(stream_id)}'>"
    )


def features_pre_auth() -> str:
    return (
        "<stream:features>"
        f"<mechanisms xmlns='{NS_SASL}'><mechanism>PLAIN</mechanism></mechanisms>"
        "</stream:features>"
    )


def features_post_auth() -> str:
    return (
        "<stream:features>"
        f"<bind xmlns='{NS_BIND}'/>"
        f"<session xmlns='{NS_SESSION}'/>"
        "</stream:features>"
    )


def sasl_success() -> str:
    return f"<success xmlns='{NS_SASL}'/>"


def iq_result_bind(iq_id: str, bound_jid: str) -> str:
    return (
        f"<iq type='result' id='{html.escape(iq_id)}'>"
        f"<bind xmlns='{NS_BIND}'><jid>{html.escape(bound_jid)}</jid></bind>"
        "</iq>"
    )


def iq_result_empty(iq_id: str) -> str:
    return f"<iq type='result' id='{html.escape(iq_id)}'/>"


def message_chat(from_jid: str, to_jid: str, body: str, msg_id: str) -> str:
    return (
        f"<message type='chat' id='{html.escape(msg_id)}' "
        f"from='{html.escape(from_jid)}' to='{html.escape(to_jid)}'>"
        f"<body>{html.escape(body)}</body>"
        "</message>"
    )


def parse_xml_element(xml_text: str) -> ET.Element:
    return ET.fromstring(xml_text)
