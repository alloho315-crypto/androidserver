package com.example.localxmpp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MediatorScreen(vm: MediatorViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Local XMPP Mediator", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = vm.recipient,
            onValueChange = { vm.recipient = it },
            label = { Text("Recipient JID / Number") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = vm.messageBody,
            onValueChange = { vm.messageBody = it },
            label = { Text("Message body") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::startServer) { Text("Start Local Server") }
            Button(onClick = vm::sendToClient) { Text("Send") }
        }

        Text("Inbound messages from chat client:")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(vm.inboundLog) { msg ->
                Text("[${msg.fromJid} -> ${msg.toJid}] ${msg.body}")
            }
        }
    }
}
