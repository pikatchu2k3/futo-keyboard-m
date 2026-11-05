# FUTO Keyboard

> **Futo Voice Mod** — FUTO Keyboard fork adding external offline speech recognition via RECOGNIZE_SPEECH popup — no mic permission, no system IME switch, no Shizuku/ADB. Auto-syncs upstream + builds + releases.

The goal is to make a good modern keyboard that stays offline and doesn't spy on you. This keyboard is a fork of [LatinIME, The Android Open-Source Keyboard](https://android.googlesource.com/platform/packages/inputmethods/LatinIME), with significant changes made to it.

Check out the [FUTO Keyboard website](https://keyboard.futo.tech/) for downloads and more information.

The code is licensed under the [FUTO Source First License 1.1](LICENSE.md).

## Nightly builds

Usually, a build close to the current master is available at [https://dl.keyboard.futo.org/nightly.apk](https://dl.keyboard.futo.org/nightly.apk). Read more about it here: https://docs.keyboard.futo.tech/improvements/nightly

The "Check for updates manually" button in the nightly build will check against the latest nightly, allowing you to get updated this way. It's recommended to check it once immediately after first installation in case an older version got downloaded due to CDN cache.

## Changelog

### v2026.11.06
- ✨ **Neu — Bunte Tasten-Popups mit Feuerwerk:** neue Einstellung „Bunte Popups mit Feuerwerk" (standardmäßig aus). Tasten-Popups bekommen einen leuchtenden Farbverlauf, dessen Farbe bei jedem Tastendruck wechselt; jeder Tastendruck löst ein kleines Partikel-Feuerwerk aus (max. 600 Partikel gleichzeitig).
- 🇩🇪 **Deutsche Texte** für die neuen Funktionen in `java/res/values-de/strings-futo-m.xml` — die Upstream-Übersetzungen im `translations`-Submodul bleiben unberührt.
- 🔧 Upstream-Sync: FUTO `master` @ `7d61ab413`.

### v2026.11.05
- ✨ **Neu — Angepinnte Clips sortieren:** im Zwischenablage-Manager (QuickClip) per Button in der Titelleiste oder über die Einstellung „Angepinnte Clips sortieren". Drei Modi: **Individuell** (↑/↓-Pfeile verschieben den Clip), **Alphabetisch** (Groß-/Kleinschreibung ignoriert, Umlaute korrekt einsortiert, Bilder am Ende) und **Nach Datum** (neueste zuerst).
- Das Einfügen eines angepinnten Clips ändert seinen Zeitstempel nicht mehr; bei „Individuell" behält ein erneut kopierter Clip seinen Platz.

### v2026.08.31
- 🐛 **Fixed:** Text über den integrierten Zwischenablage-Manager (QuickClip / Clipboard-Verlauf) einfügen und dann einmal **Backspace** drücken löschte vorher den kompletten eingefügten Text. Jetzt wird nur noch das letzte Zeichen gelöscht (wie bei normalem Einfügen) — der Clipboard-Text wird direkt per `commitText` eingefügt statt über den `mEnteredText`-Mehrzeichen-Pfad (des `mBackspaceDeletesInsertedText`-Mechanismus für Smiley-/`.com`-Tasten).

## Installation (dieser Fork)

Fertige Builds liegen als GitHub-Release vor: [Releases](https://github.com/pikatchu2k3/futo-keyboard-m/releases).

- **Obtainium:** „Add App" → GitHub → `https://github.com/pikatchu2k3/futo-keyboard-m`, APK-Filter `*-unstable-release.apk`. Updates kommen automatisch über neue Release-Tags.
- **applicationId:** `org.futo.inputmethod.latin.shizuku` — der Fork läuft **parallel** zur offiziellen FUTO-Tastatur (`org.futo.inputmethod.latin`), ohne diese zu überschreiben. Nur `arm64-v8a`.
- Der Build läuft automatisch: sonntags wird Upstream synchronisiert, gebaut und ein Release erzeugt (`.github`-frei, per Skript `scripts/update-build-release.sh` auf einem Heim-Server).

## Issue tracking and contributing

Please check the GitHub repository to report issues: [https://github.com/futo-org/android-keyboard/](https://github.com/futo-org/android-keyboard/)

The source code is hosted on our [internal GitLab](https://gitlab.futo.org/keyboard/latinime) and mirrored to [GitHub](https://github.com/futo-org/android-keyboard/). As registration is closed on our internal GitLab, we use GitHub instead for issues and pull requests.

If you want to help translate the app, please do so via our Pontoon instance: https://i18n-keyboard.futo.org/

Due to custom license, pull requests to this repository require signing a [CLA](https://cla.futo.org/) which you can do after opening a PR. Contributions to the [layouts repo](https://github.com/futo-org/futo-keyboard-layouts) don't require CLA as they're Apache-2.0

Please do not submit AI-generated pull requests, as these tend to make the codebase more difficult to understand. Pull requests made with clear heavy use of AI may be closed without comment. Please also avoid using AI to write issues, and try to explain it in your own words instead.

## Layouts

If you want to contribute layouts, check out the [layouts repo](https://github.com/futo-org/futo-keyboard-layouts).

## Building

When cloning the repository, you must perform a recursive clone to fetch all dependencies:
```
git clone --recursive https://gitlab.futo.org/keyboard/latinime.git
```

If you forgot to specify recursive clone, use this to fetch submodules:
```
git submodule update --init --recursive
```

You can then open the project in Android Studio and build it that way, or use gradle commands:
```
./gradlew assembleUnstableDebug
./gradlew assembleStableRelease
```

## APK signing

For official FUTO Keyboard versions, you can verify the APK's signing key fingerprint for integrity.

```
Signing key fingerprint for all versions except Google Play:

MD5: 3A:BB:71:C6:BB:E4:92:27:B1:E3:5D:81:01:48:6A:B0
SHA1: 5D:15:B3:6E:C9:6A:96:28:41:09:DD:62:93:0D:9C:39:9F:5F:06:43
SHA-256: 74:3F:AD:58:64:AB:C4:26:50:0B:2D:C2:C4:7C:8A:D3:24:CB:CD:16:03:3F:80:16:99:48:41:35:63:74:F9:95

```
