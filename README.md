# Local Mini XMPP Node — Kotlin + Jetpack Compose

This repository now delivers the mediator as an **Android Studio Kotlin app using Jetpack Compose**.

## Architecture

```mermaid
flowchart LR
    A[XMPP Chat Client\n(local device)] -->|TCP 127.0.0.1:5222| B[LocalXmppServer]
    B --> C[XmppSession\nRFC6120/6121 flow]
    C --> D[InboundMessage callback]
    D --> E[Compose UI]
    E -->|Inject <message type='chat'>| C
```

## Protocol flow implemented

1. `<stream:stream>` open (client -> mediator)
2. `<stream:features>` (PLAIN SASL)
3. `<auth mechanism='PLAIN'>...` + `<success/>`
4. stream restart
5. `<stream:features>` (bind + session)
6. bind IQ request/result with generated local JID (`user@local.node/resource`)
7. session IQ result
8. phase-1 message routing (`<message type='chat'>`)
   - Client -> mediator UI log
   - Compose UI -> injected inbound message to client stream

## Kotlin module map

- `server/LocalXmppServer.kt`: loopback server and active session management.
- `server/XmppSession.kt`: XML stream lifecycle, SASL/bind/session, message handling.
- `protocol/XmppStanzas.kt`: stanza builders/parsers and XML escaping.
- `protocol/XmlExtractors.kt`: lightweight XML extraction helpers.
- `ui/MediatorViewModel.kt`: mediator state + bridge to server.
- `ui/MediatorScreen.kt`: Compose screen (recipient, body, send, inbound log).
- `MainActivity.kt`: Compose app entry and lifecycle wiring.

## Run

1. Open in Android Studio.
2. Build & run app on device/emulator.
3. Configure XMPP client to connect to:
   - Host `127.0.0.1`
   - Port `5222`
   - Domain `local.node`

## Note on old Python files

`local_xmpp/` is retained as a compatibility stub in git history; active implementation is Kotlin Compose.
