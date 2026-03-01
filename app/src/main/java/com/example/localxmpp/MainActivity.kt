package com.example.localxmpp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.localxmpp.ui.MediatorScreen
import com.example.localxmpp.ui.MediatorViewModel

/**
 * Entry point for mediator UI and lifecycle owner for local XMPP server.
 */
class MainActivity : ComponentActivity() {
    private val vm = MediatorViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediatorScreen(vm)
        }
    }

    override fun onDestroy() {
        vm.stopServer()
        super.onDestroy()
    }
}
