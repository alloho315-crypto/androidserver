package com.example.localxmpp.server

import com.example.localxmpp.model.InboundMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.ServerSocket

/**
 * Local-only XMPP server that accepts a single active client on 127.0.0.1.
 */
class LocalXmppServer(
    private val host: String = "127.0.0.1",
    private val port: Int = 5222,
    private val domain: String = "local.node"
) {
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var activeSession: XmppSession? = null

    fun start(scope: CoroutineScope, onInboundMessage: (InboundMessage) -> Unit) {
        if (serverSocket != null) return
        serverSocket = ServerSocket(port, 1, InetAddress.getByName(host))

        acceptJob = scope.launch(Dispatchers.IO) {
            val socket = serverSocket!!.accept()
            val session = XmppSession(socket = socket, domain = domain, onInboundMessage = onInboundMessage)
            activeSession = session
            session.run()
        }
    }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        serverSocket?.close()
        serverSocket = null
        activeSession = null
    }

    fun inject(senderJid: String, body: String, scope: CoroutineScope) {
        val session = activeSession ?: return
        scope.launch(Dispatchers.IO) {
            session.injectInboundToClient(senderJid, body)
        }
    }
}
