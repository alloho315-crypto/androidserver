package com.example.localxmpp.model

/**
 * Message intercepted from the connected XMPP chat client.
 */
data class InboundMessage(
    val fromJid: String,
    val toJid: String,
    val body: String
)
