# Changelog

## 1.0.0

First release.

- Real-time transcription of Simple Voice Chat speech, with live (partial) and final results
- Speech bubbles above speakers (text displays), visible only to players who can hear the speaker:
  proximity distance, whispering, groups
- Up to 3 stacked bubbles, word limit per bubble, fade-out, copy of finished phrases in chat
- Bubble styles: light / dark / glass / minimal presets, colors, tail, alignment, padding, scale
- Engines: Vosk (offline), T-one (offline, Russian), Deepgram and OpenAI (cloud); `/pvt engine`, `/pvt language`
- Numbers and math words shown as digits and symbols (Russian), nickname matching for online players
- Same developer API as PV-Transcribe (Plasmo Voice), so plugins work with both voice chats
- Developer API: speech start/end, partial/final transcripts, phrase triggers, subtitle processors, Bukkit events
- Permissions, per-player and per-world switches, English and Russian messages
