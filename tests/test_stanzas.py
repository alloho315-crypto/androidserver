import base64

from local_xmpp import stanzas


def test_plain_auth_parsing():
    payload = base64.b64encode(b"authz\x00alice\x00secret").decode()
    auth = stanzas.parse_plain_auth(payload)
    assert auth.authcid == "alice"
    assert auth.password == "secret"


def test_message_chat_xml_contains_required_fields():
    xml = stanzas.message_chat("alice@local.node/a", "bob@local.node/b", "hello", "m1")
    assert "type='chat'" in xml
    assert "from='alice@local.node/a'" in xml
    assert "to='bob@local.node/b'" in xml
    assert "<body>hello</body>" in xml
