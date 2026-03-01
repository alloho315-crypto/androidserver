# Local Mini XMPP Node (Android / Kotlin)

This project is now implemented for **Android Studio (Kotlin)** as a local standalone XMPP mediator that runs fully offline on one device.

## High-Level Architecture

```mermaid
flowchart LR
    A[XMPP Client App\n(e.g., Gajim/Kaidan)] -->|TCP 127.0.0.1:5222| B[LocalXmppServer]
    B --> C[XmppSession\nRFC6120 stream/auth/bind/session]
    C --> D[InboundMessage callback]
    D --> E[Mediator Compose UI]
    E -->|Inject chat stanza| C
```

## Detailed Protocol Flow

1. Client opens TCP socket to `127.0.0.1:5222`.
2. Client sends `<stream:stream ...>`.
3. Mediator returns stream header + features with SASL PLAIN.
4. Client sends `<auth mechanism='PLAIN'>...</auth>`.
5. Mediator validates/decodes payload and returns `<success/>`.
6. Client reopens stream.
7. Mediator returns features with `<bind/>` and `<session/>`.
8. Client sends bind IQ; mediator returns bound JID (`user@local.node/resource`).
9. Client sends session IQ; mediator returns IQ result.
10. Message loop starts:
    - Client → Mediator: `<message type='chat'>` parsed into `InboundMessage` and shown in UI log.
    - Mediator UI → Client: compose send action injects valid `<message type='chat'>` into active stream.

## Source Layout

- `app/src/main/java/com/example/localxmpp/server/LocalXmppServer.kt`
- `app/src/main/java/com/example/localxmpp/server/XmppSession.kt`
- `app/src/main/java/com/example/localxmpp/protocol/XmppStanzas.kt`
- `app/src/main/java/com/example/localxmpp/protocol/XmlExtractors.kt`
- `app/src/main/java/com/example/localxmpp/ui/MediatorViewModel.kt`
- `app/src/main/java/com/example/localxmpp/ui/MediatorScreen.kt`
- `app/src/main/java/com/example/localxmpp/MainActivity.kt`

## Run (Android Studio)

1. Open the project in Android Studio Hedgehog+.
2. Run on an emulator or device.
3. Configure your XMPP chat client to connect to:
   - Host: `127.0.0.1`
   - Port: `5222`
   - Domain: `local.node`

> Note: loopback behavior depends on how the client app is deployed (same app process, emulator mapping, or local VPN/tunnel). The mediator code itself is local-only and never reaches external network endpoints.

## Extensibility

`XmppSession.run()` has an explicit hook where IQ/presence branches can be expanded after Phase 1 message support.
