## SVC-Transcribe 1.0.0

First release: real-time speech-to-text for Simple Voice Chat with speech bubbles above players. Built on the same core as PV-Transcribe; it has not been tested in game with Simple Voice Chat yet, please report issues.

### Downloads

`SVC-Transcribe-1.0.0.jar` is attached below.

### Compatibility

| | Supported |
|---|---|
| Server | Paper, Spigot, Purpur **1.20.2 – 1.21.11** (tested on 1.21.8) |
| Java | 17 or newer |
| Voice chat | Simple Voice Chat **2.5+** (Bukkit/Paper) |
| Players | only the Simple Voice Chat mod, nothing extra |
| Not supported | Folia, Fabric/Forge servers, Plasmo Voice (use PV-Transcribe) |

On first start the server downloads the Vosk / ONNX Runtime libraries from Maven Central, and the plugin downloads the speech model.

### Installation

1. Install Simple Voice Chat on the server.
2. Put `SVC-Transcribe-1.0.0.jar` into `plugins/` and start the server.
3. Optional: for the best accuracy set a Deepgram key (`engine.deepgram.api-key`) and run `/vtt engine deepgram`.

### Changes

- Real-time speech-to-text with live (partial) and final results
- Speech bubbles above speakers (vanilla text displays, no client mod), shown only to players who can hear the speaker: proximity, whispering, groups
- Up to 3 stacked bubbles, word limit per bubble, fade-out, copy of finished phrases in chat
- Bubble styles: `light`, `dark`, `glass`, `minimal` presets, plus colors, tail, alignment, padding and size
- Speech engines: Vosk (offline), T-one (offline, Russian), Deepgram and OpenAI (cloud), switchable with `/vtt engine`
- `/vtt set <option> <value>` changes any config option in game (Tab completion); `/vtt get` shows them
- `/vtt demo` shows a demo bubble without a microphone
- Numbers and math as digits and symbols (Russian), nickname matching for online players
- Developer API shared with PV-Transcribe (Plasmo Voice): speech start/end, partial/final transcripts, phrase triggers, subtitle processors, Bukkit events
- English and Russian messages (`locale: en|ru`)

Full documentation: [README](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe#readme) · [Русский](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/blob/main/README.ru.md)
