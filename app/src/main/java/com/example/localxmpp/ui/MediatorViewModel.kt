package com.example.localxmpp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.localxmpp.model.InboundMessage
import com.example.localxmpp.server.LocalXmppServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * UI state holder connecting Compose to the local XMPP server.
 */
class MediatorViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val xmppServer = LocalXmppServer()

    var recipient by mutableStateOf("peer@local.node")
    var messageBody by mutableStateOf("")
    val inboundLog = mutableStateListOf<InboundMessage>()

    fun startServer() {
        xmppServer.start(scope) { msg -> inboundLog.add(msg) }
    }

    fun stopServer() {
        xmppServer.stop()
        scope.cancel()
    }

    fun sendToClient() {
        if (messageBody.isBlank()) return
        xmppServer.inject(senderJid = recipient, body = messageBody, scope = scope)
        messageBody = ""
    }
}
