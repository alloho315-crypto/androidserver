package com.example.localxmpp.protocol

import org.junit.Assert.assertTrue
import org.junit.Test

class XmppStanzasTest {
    @Test
    fun chatMessage_hasExpectedFields() {
        val xml = XmppStanzas.chatMessage("alice@local.node/a", "bob@local.node/b", "hello", "m1")
        assertTrue(xml.contains("type='chat'"))
        assertTrue(xml.contains("from='alice@local.node/a'"))
        assertTrue(xml.contains("to='bob@local.node/b'"))
        assertTrue(xml.contains("<body>hello</body>"))
    }

    @Test
    fun featuresPreAuth_advertisesPlain() {
        val xml = XmppStanzas.featuresPreAuth()
        assertTrue(xml.contains("<mechanism>PLAIN</mechanism>"))
    }
}
