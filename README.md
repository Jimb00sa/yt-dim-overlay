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
