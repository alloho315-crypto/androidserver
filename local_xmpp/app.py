from __future__ import annotations

import argparse
import asyncio

from .stream import LocalXmppServer
from .ui import MediatorUi


async def run(args: argparse.Namespace) -> None:
    xmpp = LocalXmppServer(host=args.xmpp_host, port=args.xmpp_port, domain=args.domain)
    ui = MediatorUi(xmpp_server=xmpp, host=args.ui_host, port=args.ui_port)

    await xmpp.start()
    await ui.start()

    print(f"XMPP local node listening on {args.xmpp_host}:{args.xmpp_port} for domain {args.domain}")
    print(f"Mediator UI on http://{args.ui_host}:{args.ui_port}")
    print("Press Ctrl+C to stop")

    stop_event = asyncio.Event()
    try:
        await stop_event.wait()
    except asyncio.CancelledError:
        pass


def main() -> None:
    parser = argparse.ArgumentParser(description="Local standalone XMPP mediator")
    parser.add_argument("--xmpp-host", default="127.0.0.1")
    parser.add_argument("--xmpp-port", type=int, default=5222)
    parser.add_argument("--ui-host", default="127.0.0.1")
    parser.add_argument("--ui-port", type=int, default=8080)
    parser.add_argument("--domain", default="local.node")
    args = parser.parse_args()

    try:
        asyncio.run(run(args))
    except KeyboardInterrupt:
        print("Stopped")


if __name__ == "__main__":
    main()
