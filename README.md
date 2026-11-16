# YT Dim Overlay

Small Android app for watching YouTube in bed at night: it dims everything on
screen **except the video player** while YouTube (or a Morphe / ReVanced
patched YouTube) is in front. Taps and scrolls go straight through the overlay.

## Features
- On/off switch in the app, plus a **Quick Settings tile** ("YT Dim")
- Any overlay **colour** (presets or hex) and **opacity** (0–100%)
- Adjustable **clear band** for the video (top edge + height), with live preview
- Only appears in the apps you list (package names; detects installed YouTube apps)
- Optional: hide automatically in landscape / fullscreen

## How it works
It's an `AccessibilityService` that adds a `TYPE_ACCESSIBILITY_OVERLAY`
window. That window type is trusted by Android, so touches pass through at any
opacity (normal "draw over apps" overlays block touches above 80% opacity on
Android 12+) and no overlay permission is needed. The service only checks which
app window is in front; it doesn't read or store screen content.

## Install
Obtainium: add `https://github.com/Jimb00sa/yt-dim-overlay` and enable
*"Don't track release version, use the release date/time"* (the `latest` release
is replaced on every build, so its tag never changes).

Manual:
1. Download `yt-dim-overlay.apk` from the **latest** release (or the Actions run
   artifact) and open it on the phone, or `adb install -r yt-dim-overlay.apk`.
2. Open **YT Dim Overlay** → *Open accessibility settings* → turn on
   **YT Dim Overlay**. If Android says it's a *restricted setting*: App info →
   ⋮ → *Allow restricted settings*, then try again.
3. Add the **YT Dim** tile to Quick Settings.

## Build
```
./gradlew assembleDebug    # → app/build/outputs/apk/debug/app-debug.apk
```
Every push to `main` is built by GitHub Actions. The repo includes a debug
keystore so local and CI builds have the same signature and install over each
other as updates.

## Requirements
Android 8.0 (API 26) or newer.

## Disclaimers
- **Accessibility permission.** Android describes every accessibility service in
  alarming terms (e.g. "can observe your actions"). This app only checks which
  app is in the foreground and draws a dimming window. It has no internet
  permission, collects nothing, and sends nothing anywhere. Read the source in
  `app/src/main/java/` to verify, which is the point of it being open.
- **Unofficial.** Not affiliated with or endorsed by YouTube or Google.
  "YouTube" is a trademark of Google LLC. Patched YouTube clients are your own
  business and not something this project provides or supports.
- **Debug-signed builds.** Releases are signed with the debug keystore committed
  in this repo, so **anyone** can produce an APK with the same signature. The
  signature is not proof an APK came from me. Only install APKs from this repo's
  Releases page, or build it yourself.
- **Rolling release.** `latest` is overwritten on every push to `main`.
- **No warranty.** Provided as-is, untested on most devices, and may break on
  Android or YouTube updates. Use at your own risk. See [LICENSE](LICENSE).
- **Not for safety-critical use.** It is a cosmetic dimmer; it can fail to
  appear or fail to hide.

## License
MIT
