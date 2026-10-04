<a id="en"></a>

<div align="center">

🇬🇧 **English** · [🇷🇺 Русский — ниже](#ru)

<img src="docs/images/banner-en.png" alt="SVC-Transcribe">

[Releases](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/releases) | [Modrinth](https://modrinth.com/plugin/svc-transcribe) | [Documentation](#configuration) | [API](#for-developers) | [Report a bug](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/issues) | [🇷🇺 Русский](#ru)

[![Release](https://img.shields.io/github/v/release/ArabKustam/Simple-Voice-Chat-Transcribe?label=release)](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/releases)
[![Build](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/actions/workflows/build.yml/badge.svg)](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/actions)
![Paper 1.20.2+](https://img.shields.io/badge/Paper%20%2F%20Spigot-1.20.2%E2%80%931.21.11-brightgreen)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

## See what people say in voice chat

Players talk in [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat), and everyone who can hear them sees their words above their head. The text grows word by word while they speak. Players don't need any extra mod or resource pack.

<p align="center"><img src="docs/images/live-demo-en.gif" alt="Live subtitles" width="760"></p>

## Features

![Live subtitles](docs/images/panel-1-en.png)

<p align="center"><img src="docs/images/conversation-en.gif" alt="Two players talking" width="860"></p>

![Bubble styles](docs/images/panel-2-en.png)

![Speech engines](docs/images/panel-3-en.png)

![For developers](docs/images/panel-4-en.png)

Bubbles are shown only to players who can hear the speaker: proximity distance, whispering and groups.

![How it works](docs/images/how-it-works-en.png)

## Requirements

| | |
|---|---|
| Server | Paper, Spigot or Purpur **1.20.2+**, tested on 1.21.8 |
| Java | 17 or newer |
| Voice chat | Simple Voice Chat **2.5+** (Bukkit/Paper version) |
| Players | only the Simple Voice Chat mod |
| First start | internet access: the server downloads libraries from Maven Central and the plugin downloads the speech model |

## Installation

1. Install Simple Voice Chat on the server.
2. Download `SVC-Transcribe-x.y.z.jar` from [Releases](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/releases) (or Modrinth) into `plugins/`.
3. Start the server. The plugin picks a free local engine automatically.
4. Optional, for the best accuracy, use Deepgram:
   1. Create a key at [console.deepgram.com](https://console.deepgram.com).
   2. Put it into `engine.deepgram.api-key` in `plugins/SVC-Transcribe/config.yml`.
   3. Run `/vtt reload` and `/vtt engine deepgram`.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/vtt toggle` | Hide or show bubbles for yourself | `svctranscribe.command.toggle` (everyone) |
| `/vtt status` | Engine state, load, dropped audio | `svctranscribe.admin.status` |
| `/vtt reload` | Reload config and messages | `svctranscribe.admin.reload` |
| `/vtt demo [player] [text]` | Show a demo bubble without a microphone (test styles, take screenshots) | `svctranscribe.admin.demo` |
| `/vtt get [section]` | Show settings (API keys are masked) | `svctranscribe.admin.config` |
| `/vtt set <option> <value>` | Change **any** config option in game; Tab completes options and values; saved to config.yml | `svctranscribe.admin.config` |
| `/vtt engine <auto\|vosk\|t-one\|deepgram\|openai>` | Switch the speech engine live | `svctranscribe.admin.engine` |
| `/vtt language <code\|auto>` | Recognition language, `auto` = detect | `svctranscribe.admin.engine` |
| `/vtt player <name> <on\|off>` | Enable or disable transcription for a player | `svctranscribe.admin.player` |
| `/vtt world <world> <on\|off>` | Enable or disable transcription in a world | `svctranscribe.admin.world` |

More permissions:

| Permission | Default | Meaning |
|---|---|---|
| `svctranscribe.see` | everyone | Sees bubbles |
| `svctranscribe.transcribe` | everyone | Their speech is transcribed |
| `svctranscribe.admin` | op | All admin commands |

## Configuration

Settings can be changed in two ways: edit [`config.yml`](platform-paper/src/main/resources/config.yml) and run `/vtt reload`, or change them in game with `/vtt set <option> <value>`, for example `/vtt set subtitles.style.preset dark`. Every option is documented in the config: language, engine and API keys, bubbles and their look, who sees them, timings, chat copy, number formatting, nickname matching, worlds, performance and debugging. Messages are in `plugins/SVC-Transcribe/lang/` (English and Russian).

```yaml
subtitles:
  max-bubbles: 3
  max-words-per-bubble: 10
  style:
    preset: dark              # light, dark, glass, minimal
    background: "#C81E3A8A"   # #AARRGGBB
    final-format: "&f&l{text}"
    alignment: center
    padding: 2
    scale: 1.2
    tail:
      symbol: "▾"
```

> Minecraft draws text backgrounds as rectangles, so rounded corners would need a resource pack. Everything else works on an unmodified client.

## For developers

SVC-Transcribe and [PV-Transcribe](https://github.com/ArabKustam/Plasmo-Voice-Transcribe) (Plasmo Voice) share the same API, so your plugin works with either voice chat without changes. Add the plugin jar as `compileOnly` and `softdepend: [PV-Transcribe, SVC-Transcribe]` to your `plugin.yml`.

```java
PVTranscribeApi api = PVTranscribe.get();

// Live and final text of every player
api.addListener(new TranscriptionListener() {
    @Override public void onSpeechStart(SpeechStartEvent e) { }
    @Override public void onPartialTranscript(Transcript t) { /* still speaking */ }
    @Override public void onFinalTranscript(Transcript t) { /* finished phrase */ }
    @Override public void onSpeechEnd(SpeechEndEvent e) { }
}, api.syncExecutor());

// Voice commands
api.phrases().register(PhraseTrigger.builder("magic:fireball")
        .phrases("fireball", "огненный шар")
        .mode(MatchMode.CONTAINS)
        .matchPartial(true)                // react before the sentence ends
        .executor(api.syncExecutor())      // main thread
        .handler(match -> {
            Player player = Bukkit.getPlayer(match.speakerId());
            if (player != null) player.launchProjectile(Fireball.class);
        })
        .build());

// Moderation: change or hide subtitle text
api.addSubtitleProcessor((transcript, text) -> text.replace("badword", "***"));
```

A `Transcript` has the speaker, `getText()`, `getRawText()`, `getNormalizedText()` (for matching), `isFinal()`, `getUtteranceId()`, the language, the voice source (`simplevoicechat`) and the voice channel (`proximity`, `whisper`, `group`). Bukkit events are also fired on the main thread: `PlayerSpeechStartEvent`, `PlayerTranscriptEvent`, `PlayerSpeechEndEvent`.

## FAQ

**Which Minecraft versions?**
Paper, Spigot and Purpur 1.20.2 and newer (text displays appeared in 1.19.4, smooth movement in 1.20.2). Tested on 1.21.8.

**Does it work with Plasmo Voice?**
Use [PV-Transcribe](https://github.com/ArabKustam/Plasmo-Voice-Transcribe). Install only one of the two plugins.

**How accurate is it?**
It depends on the engine. Deepgram is the most accurate: names, punctuation, digits. T-one is a good free choice for Russian. Vosk is the lightest, but it sometimes swaps rare words for similar common ones.

**Is voice recorded?**
No. Audio stays in memory only while it is being recognised. With a cloud engine the audio is sent to that provider while players speak, so tell your players about it.

## Building

```
./gradlew build          # jar in build/libs/
./gradlew runServer      # local test server
```

| Module | Contents |
|---|---|
| `api` | Public API, no Minecraft dependencies |
| `core` | Sessions, threading, subtitles, text formatting |
| `engine-*` | Speech engines |
| `voice-*` | Voice chat adapter |
| `platform-paper` | Bukkit plugin |

## Support

- Bugs and ideas: [GitHub Issues](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/issues)
- Author on Discord: **arab_kustam**
- Simple Voice Chat Discord: https://discord.gg/4dH2zwTmyX

## License

[MIT](LICENSE). Third-party components: [THIRD_PARTY.md](THIRD_PARTY.md). Version history: [CHANGELOG.md](CHANGELOG.md).

---

<a id="ru"></a>

# 🇷🇺 Описание на русском

<div align="center">

[🇬🇧 English — выше](#en) · 🇷🇺 **Русский**

<img src="docs/images/banner-ru.png" alt="SVC-Transcribe">

[Релизы](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/releases) | [Modrinth](https://modrinth.com/plugin/svc-transcribe) | [Настройка](#настройка) | [API](#для-разработчиков) | [Сообщить об ошибке](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/issues) | [🇬🇧 English](#en)

[![Release](https://img.shields.io/github/v/release/ArabKustam/Simple-Voice-Chat-Transcribe?label=release)](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/releases)
[![Build](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/actions/workflows/build.yml/badge.svg)](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/actions)
![Paper 1.20.2+](https://img.shields.io/badge/Paper%20%2F%20Spigot-1.20.2%E2%80%931.21.11-brightgreen)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

## Видно, что говорят в голосовом чате

Игроки говорят в [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat), и все, кто их слышит, видят их слова над головой. Текст растёт по словам прямо во время речи. Игрокам не нужны ни дополнительные моды, ни ресурспаки.

<p align="center"><img src="docs/images/live-demo-ru.gif" alt="Живые субтитры" width="760"></p>

## Возможности

![Живые субтитры](docs/images/panel-1-ru.png)

<p align="center"><img src="docs/images/conversation-ru.gif" alt="Разговор двух игроков" width="860"></p>

![Стили облачков](docs/images/panel-2-ru.png)

![Движки распознавания](docs/images/panel-3-ru.png)

![Для разработчиков](docs/images/panel-4-ru.png)

Облачка видят только те, кто слышит говорящего: дальность голоса, шёпот и группы.

![Как это работает](docs/images/how-it-works-ru.png)

## Требования

| | |
|---|---|
| Сервер | Paper, Spigot или Purpur **1.20.2+**, проверено на 1.21.8 |
| Java | 17 и новее |
| Голосовой чат | Simple Voice Chat **2.5+** (Bukkit/Paper version) |
| Игрокам | только мод Simple Voice Chat |
| Первый запуск | нужен интернет: сервер скачает библиотеки, плагин скачает модель распознавания |

## Установка

1. Установите Simple Voice Chat на сервер.
2. Скачайте `SVC-Transcribe-x.y.z.jar` из [Releases](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/releases) (или с Modrinth) в `plugins/`.
3. Запустите сервер. Плагин сам выберет бесплатный локальный движок.
4. Для русского языка впишите в `plugins/SVC-Transcribe/config.yml` строки `locale: ru` и `transcription.language: ru`.
5. По желанию, для максимальной точности, подключите Deepgram:
   1. Создайте ключ на [console.deepgram.com](https://console.deepgram.com).
   2. Впишите его в `engine.deepgram.api-key`.
   3. Выполните `/vtt reload` и `/vtt engine deepgram`.

## Команды

| Команда | Что делает | Право |
|---|---|---|
| `/vtt toggle` | Скрыть или показать облачка для себя | `svctranscribe.command.toggle` (все) |
| `/vtt status` | Состояние движка, нагрузка, потерянный звук | `svctranscribe.admin.status` |
| `/vtt reload` | Перезагрузить конфиг и сообщения | `svctranscribe.admin.reload` |
| `/vtt demo [игрок] [текст]` | Демо-облачко без микрофона (проверить стиль, сделать скриншот) | `svctranscribe.admin.demo` |
| `/vtt get [раздел]` | Показать настройки (ключи API скрыты) | `svctranscribe.admin.config` |
| `/vtt set <параметр> <значение>` | Изменить **любой** параметр конфига прямо в игре; Tab подсказывает параметры и значения; сохраняется в config.yml | `svctranscribe.admin.config` |
| `/vtt engine <auto\|vosk\|t-one\|deepgram\|openai>` | Сменить движок на лету | `svctranscribe.admin.engine` |
| `/vtt language <код\|auto>` | Язык распознавания, `auto` — автоопределение | `svctranscribe.admin.engine` |
| `/vtt player <ник> <on\|off>` | Включить или выключить транскрибацию игрока | `svctranscribe.admin.player` |
| `/vtt world <мир> <on\|off>` | Включить или выключить транскрибацию в мире | `svctranscribe.admin.world` |

Другие права:

| Право | По умолчанию | Значение |
|---|---|---|
| `svctranscribe.see` | все | Видеть облачка |
| `svctranscribe.transcribe` | все | Речь игрока распознаётся |
| `svctranscribe.admin` | op | Все админские команды |

## Настройка

Настройки меняются двумя способами: в [`config.yml`](platform-paper/src/main/resources/config.yml) с последующим `/vtt reload` или прямо в игре командой `/vtt set <параметр> <значение>`, например `/vtt set subtitles.style.preset dark`. Все параметры описаны в конфиге: язык, движок и ключи API, облачка и их вид, кто их видит, тайминги, копия в чат, цифры, ники, миры, производительность и отладка. Полный конфиг с комментариями на русском: [docs/config.ru.md](docs/config.ru.md). Тексты сообщений лежат в `plugins/SVC-Transcribe/lang/` (английский и русский).

```yaml
subtitles:
  max-bubbles: 3
  max-words-per-bubble: 10
  style:
    preset: dark              # light, dark, glass, minimal
    background: "#C81E3A8A"   # #AARRGGBB
    final-format: "&f&l{text}"
    alignment: center
    padding: 2
    scale: 1.2
    tail:
      symbol: "▾"
```

> Фон текста Minecraft рисует только прямоугольным, скруглённые углы возможны только с ресурспаком. Всё остальное работает на обычном клиенте.

## Для разработчиков

У SVC-Transcribe и [PV-Transcribe](https://github.com/ArabKustam/Plasmo-Voice-Transcribe) (Plasmo Voice) общий API, поэтому ваш плагин работает с любым голосовым чатом без изменений. Подключите jar плагина как `compileOnly` и добавьте `softdepend: [PV-Transcribe, SVC-Transcribe]` в `plugin.yml`.

```java
PVTranscribeApi api = PVTranscribe.get();

// Живой и итоговый текст каждого игрока
api.addListener(new TranscriptionListener() {
    @Override public void onSpeechStart(SpeechStartEvent e) { }
    @Override public void onPartialTranscript(Transcript t) { /* ещё говорит */ }
    @Override public void onFinalTranscript(Transcript t) { /* фраза закончена */ }
    @Override public void onSpeechEnd(SpeechEndEvent e) { }
}, api.syncExecutor());

// Голосовые команды
api.phrases().register(PhraseTrigger.builder("magic:fireball")
        .phrases("fireball", "огненный шар")
        .mode(MatchMode.CONTAINS)
        .matchPartial(true)                // срабатывает, не дожидаясь конца фразы
        .executor(api.syncExecutor())      // главный поток
        .handler(match -> {
            Player player = Bukkit.getPlayer(match.speakerId());
            if (player != null) player.launchProjectile(Fireball.class);
        })
        .build());

// Модерация: изменить или скрыть текст облачка
api.addSubtitleProcessor((transcript, text) -> text.replace("плохоеслово", "***"));
```

В `Transcript` есть говорящий, `getText()`, `getRawText()`, `getNormalizedText()` (для сравнения), `isFinal()`, `getUtteranceId()`, язык, источник голоса (`simplevoicechat`) и канал голоса (`proximity`, `whisper`, `group`). Также в главном потоке вызываются события Bukkit: `PlayerSpeechStartEvent`, `PlayerTranscriptEvent`, `PlayerSpeechEndEvent`.

| Метод / класс | Для чего |
|---|---|
| `PVTranscribe.get()` | Получить API (одинаково в обоих плагинах) |
| `addListener(listener, executor)` | Начало и конец речи, промежуточный и итоговый текст |
| `phrases().register(...)` | Реакция на слова и фразы (голосовые команды) |
| `addSubtitleProcessor(...)` | Изменить текст облачка перед показом или скрыть его (вернуть `null`) |
| `syncExecutor()` | Выполнять обработчики в главном потоке сервера |

## Частые вопросы

**Для каких версий?**
Paper, Spigot и Purpur 1.20.2 и новее (текстовые дисплеи появились в 1.19.4, плавное движение в 1.20.2). Проверено на 1.21.8.

**Работает ли с Plasmo Voice?**
Для него есть отдельный плагин [PV-Transcribe](https://github.com/ArabKustam/Plasmo-Voice-Transcribe). Ставьте только один из двух.

**Насколько точно?**
Зависит от движка. Deepgram самый точный: ники, пунктуация, цифры. T-one — хороший бесплатный вариант для русского. Vosk самый лёгкий, но иногда заменяет редкие слова на похожие частые.

**Записывается ли голос?**
Нет. Звук хранится только в памяти, пока распознаётся. С облачным движком звук отправляется провайдеру, пока игрок говорит, и об этом стоит предупредить игроков.

## Сборка

```
./gradlew build          # jar в build/libs/
./gradlew runServer      # тестовый сервер
```

| Модуль | Что внутри |
|---|---|
| `api` | Публичный API, без зависимостей от Minecraft |
| `core` | Сессии, потоки, облачка, обработка текста |
| `engine-*` | Движки распознавания |
| `voice-*` | Адаптер голосового чата |
| `platform-paper` | Плагин Bukkit |

## Поддержка

- Ошибки и идеи: [GitHub Issues](https://github.com/ArabKustam/Simple-Voice-Chat-Transcribe/issues)
- Автор в Discord: **arab_kustam**
- Discord Simple Voice Chat: https://discord.gg/4dH2zwTmyX

## Лицензия

[MIT](LICENSE). Сторонние компоненты: [THIRD_PARTY.md](THIRD_PARTY.md). История версий: [CHANGELOG.ru.md](CHANGELOG.ru.md).
