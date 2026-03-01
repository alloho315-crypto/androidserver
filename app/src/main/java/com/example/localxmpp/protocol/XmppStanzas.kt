package com.example.localxmpp.protocol

import android.util.Base64

/** RFC namespace constants used by local XMPP node. */
object XmppNs {
    const val SASL = "urn:ietf:params:xml:ns:xmpp-sasl"
    const val BIND = "urn:ietf:params:xml:ns:xmpp-bind"
    const val SESSION = "urn:ietf:params:xml:ns:xmpp-session"
}

data class PlainAuth(val authzid: String, val authcid: String, val password: String)

/** Centralized stanza builders/parsers to keep stream logic clean. */
object XmppStanzas {
    fun parsePlainAuth(payloadB64: String): PlainAuth {
        val raw = String(Base64.decode(payloadB64.trim(), Base64.DEFAULT), Charsets.UTF_8)
        val pieces = raw.split('\u0000')
        require(pieces.size >= 3) { "Invalid SASL PLAIN payload" }
        return PlainAuth(pieces[0], pieces[1], pieces[2])
    }

    fun streamHeader(domain: String, streamId: String): String =
        "<?xml version='1.0'?><stream:stream from='${esc(domain)}' xmlns='jabber:client' " +
            "xmlns:stream='http://etherx.jabber.org/streams' version='1.0' id='${esc(streamId)}'>"

    fun featuresPreAuth(): String =
        "<stream:features><mechanisms xmlns='${XmppNs.SASL}'><mechanism>PLAIN</mechanism></mechanisms></stream:features>"

    fun featuresPostAuth(): String =
        "<stream:features><bind xmlns='${XmppNs.BIND}'/><session xmlns='${XmppNs.SESSION}'/></stream:features>"

    fun saslSuccess(): String = "<success xmlns='${XmppNs.SASL}'/>"

    fun iqResultBind(iqId: String, boundJid: String): String =
        "<iq type='result' id='${esc(iqId)}'><bind xmlns='${XmppNs.BIND}'><jid>${esc(boundJid)}</jid></bind></iq>"

    fun iqResultEmpty(iqId: String): String = "<iq type='result' id='${esc(iqId)}'/>"

    fun chatMessage(fromJid: String, toJid: String, body: String, msgId: String): String =
        "<message type='chat' id='${esc(msgId)}' from='${esc(fromJid)}' to='${esc(toJid)}'><body>${esc(body)}</body></message>"

    private fun esc(input: String): String =
        input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
}
