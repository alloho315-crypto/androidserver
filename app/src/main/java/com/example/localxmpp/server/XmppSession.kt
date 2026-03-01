package com.example.localxmpp.server

import com.example.localxmpp.model.InboundMessage
import com.example.localxmpp.protocol.XmlExtractors
import com.example.localxmpp.protocol.XmppStanzas
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.Socket
import java.util.UUID

/**
 * Handles one local XMPP client stream lifecycle:
 * stream open -> SASL PLAIN -> stream restart -> bind -> session -> message loop.
 */
class XmppSession(
    private val socket: Socket,
    private val domain: String,
    private val onInboundMessage: (InboundMessage) -> Unit
) {
    private val input = BufferedInputStream(socket.getInputStream())
    private val output = BufferedOutputStream(socket.getOutputStream())
    private val sendLock = Mutex()

    private var authcid: String = "localuser"
    private var boundJid: String? = null
    private val streamId: String = UUID.randomUUID().toString().replace("-", "")

    suspend fun run() {
        expectStreamOpen()
        send(XmppStanzas.streamHeader(domain, streamId))
        send(XmppStanzas.featuresPreAuth())

        val authXml = readStanzaXml()
        require(authXml.contains("<auth")) { "Expected SASL auth stanza" }
        val mechanism = XmlExtractors.attr(authXml, "mechanism")
        require(mechanism == "PLAIN") { "Only SASL PLAIN is supported" }
        val payload = authXml.substringAfter(">", "").substringBefore("</auth>")
        authcid = XmppStanzas.parsePlainAuth(payload).authcid.ifBlank { "localuser" }
        send(XmppStanzas.saslSuccess())

        expectStreamOpen()
        send(XmppStanzas.streamHeader(domain, streamId))
        send(XmppStanzas.featuresPostAuth())

        val bindIq = readStanzaXml()
        val bindId = XmlExtractors.attr(bindIq, "id") ?: error("bind iq missing id")
        val resource = XmlExtractors.bindResource(bindIq) ?: "android"
        boundJid = "$authcid@$domain/$resource"
        send(XmppStanzas.iqResultBind(bindId, boundJid!!))

        val sessionIq = readStanzaXml()
        val sessionId = XmlExtractors.attr(sessionIq, "id") ?: error("session iq missing id")
        send(XmppStanzas.iqResultEmpty(sessionId))

        while (!socket.isClosed) {
            val stanza = readStanzaXml()
            if (stanza.contains("<message") && (XmlExtractors.attr(stanza, "type") ?: "chat") == "chat") {
                onInboundMessage(
                    InboundMessage(
                        fromJid = XmlExtractors.attr(stanza, "from") ?: "",
                        toJid = XmlExtractors.attr(stanza, "to") ?: "",
                        body = XmlExtractors.body(stanza)
                    )
                )
            }
            // Presence and IQ hooks can be added here in phase 2.
        }
    }

    suspend fun injectInboundToClient(fromJid: String, messageBody: String) {
        val to = boundJid ?: return
        send(XmppStanzas.chatMessage(fromJid, to, messageBody, UUID.randomUUID().toString()))
    }

    private fun expectStreamOpen() {
        val xml = readUntil('>')
        require(xml.contains("stream:stream")) { "Expected stream:stream opening" }
    }

    private fun readStanzaXml(): String {
        val buf = StringBuilder()
        while (true) {
            val c = input.read()
            if (c == -1) error("XMPP socket closed")
            buf.append(c.toChar())

            val candidate = buf.toString().trim()
            if (candidate.startsWith("<") && candidate.endsWith(">")) {
                if (candidate.startsWith("<iq") && candidate.contains("</iq>")) return candidate
                if (candidate.startsWith("<message") && candidate.contains("</message>")) return candidate
                if (candidate.startsWith("<presence") && candidate.contains("</presence>")) return candidate
                if (candidate.startsWith("<auth") && candidate.contains("</auth>")) return candidate
                if (candidate.startsWith("<iq") && candidate.endsWith("/>")) return candidate
            }
        }
    }

    private fun readUntil(ch: Char): String {
        val buf = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) error("socket closed")
            val c = b.toChar()
            buf.append(c)
            if (c == ch) return buf.toString()
        }
    }

    private suspend fun send(xml: String) {
        sendLock.withLock {
            output.write(xml.toByteArray(Charsets.UTF_8))
            output.flush()
        }
    }
}
