# WSJT-Y Remote (Android)

Companion app for [WSJT-Y](https://github.com/2E0LXY/wsjt-zii). Connects to
[wsjty-relay](https://github.com/2E0LXY/wsjty-relay) over WSS and:

- Shows the live decode stream (callsigns heard, SNR, frequency, message)
- Tap a CQ line to reply — QSYs, populates the callsign, and starts calling
  on the desktop, same as double-clicking a station on the desktop's DX
  Station Map
- Shows current band/mode/DX call/Tx state
- Band-change buttons
- Halt Tx

No waterfall/spectrum — text-only protocol, by design, to keep bandwidth
and phone-side processing minimal. No separate logging either — QSOs are
logged by WSJT-Y itself, same as always; the app just shows a toast-style
event when one gets logged.

## Setup

Two ways to connect — pick one, or configure both (WSJT-Y can run either
or both at once):

**Relay (works anywhere, no router config):**
1. Deploy [wsjty-relay](https://github.com/2E0LXY/wsjty-relay), create a
   station: `./wsjty_relay -add-station "My Shack"` — prints a token.
2. WSJT-Y: **Tools → Configure Remote Control**, enter the relay's
   `wss://` URL and the token.
3. This app: enter the same `wss://` URL and token.

**Direct (same LAN, or WAN with a port forwarded on your router):**
1. WSJT-Y: **Tools → Configure Remote Control** — leave the relay URL
   blank (or set it too, doesn't matter), set a token, then give it a
   port for direct mode. It'll show you the LAN IP(s) to use.
2. This app: enter `ws://<that IP>:<port>` and the same token.
3. For WAN access, forward that port (TCP) on your router to the shack
   PC, then use your WAN IP or a DDNS hostname the same way.

Direct mode is plain `ws://` — not encrypted in transit, unlike the
relay's `wss://`. The auth token still gates access either way, but a
WAN-exposed direct connection is lower-assurance than the relay (which
terminates TLS via Caddy). LAN-only direct use has the same trust model
as the desktop app's existing local UDP protocol (JTAlert/GridTracker
etc. — also unencrypted on the LAN).

## Build

```
./gradlew assembleDebug
```

Requires JDK 17 and the Android SDK (compileSdk 34, minSdk 26).

## Architecture

- `data/RelayConnection.kt` — the single OkHttp WebSocket, auto-reconnects
  on drop (4s fixed delay), exposes `StateFlow`s for connection state,
  latest station status, and the decode list.
- `data/Protocol.kt` — hand-written mirror of wsjty-relay's JSON message
  shapes (see that repo's README for the authoritative protocol
  definition). Unknown message types are ignored, matching the desktop
  side, so the wire protocol can grow without breaking either end.
- `data/SettingsStore.kt` — Jetpack DataStore persistence for the relay
  URL/token pairing.
- `ui/MainViewModel.kt` — wires the above together; also does the
  best-effort callsign/grid extraction from a CQ message's text for the
  reply action, since the relay protocol sends the raw decoded message
  rather than pre-parsed fields.
- `ui/screens/` — Pairing screen (first run / re-pair) and the main screen
  (status card, band row, decode list).

## What's deliberately not here

- No background service keeping the socket alive when the app isn't in
  the foreground — first-cut scope was "works while the app is open".
  Worth adding later if you want push-style alerts while the phone's
  screen is off.
- No multi-station switcher — one pairing at a time. Fine for a
  single-operator setup; the relay itself already supports multiple
  stations if that ever changes.
