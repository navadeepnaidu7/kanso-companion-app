# Kanso Companion

[MIT licensed](LICENSE).

A native Android remote for Kanso. Opens directly to a rectangular trackpad; switch to directional buttons, or open the animated app drawer. Built with Kotlin and Jetpack Compose, using the launcher's palette and spring tuning.

The interface uses graphite surfaces, a four-app dock, and a library of wide TV artwork tiles. Hold any app to add it to the dock, replace a slot, remove it, or move it left/right. Dock choices are saved separately for each paired TV. Streaming apps lead the library; utilities follow alphabetically. Search is available from the library's magnifying glass.

Artwork is decoded off the main thread and cached before display, so scrolling does not repeatedly decode icons. TV banners use compressed WebP; apps without a banner use their original icon against a color-matched background. A private per-TV disk cache restores the library while it refreshes. The library keeps its position when closed and reopened. Dock reordering uses spring placement animations.

Artwork uses a separate authenticated connection so a library refresh cannot hold up remote key commands. `:app:assembleRelease` produces the optimized personal sideload build, signed with the same development key as the debug build to preserve pairing and favorites during updates. A store distribution would require its own release signing setup.

An app tile responds on press and shows progress while its launch request is pending. Once the TV accepts the launch, the library closes into an app-aware remote. The receiver reports the foreground app every 1.2 seconds while the companion is active, including changes made with the physical remote. System overlays do not replace the foreground app. The remote's accent, heading, and media controls adapt to the app; it does not claim to know playback progress or play/pause state.

## Run on a phone

Requires JDK 25 (the Gradle daemon toolchain can provision it) and Android SDK 37. The phone needs Android 9 or later.

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.navadeep.kanso.companion/.MainActivity
```

The first screen is usable without a TV. **Connect → Try preview** enables an explicitly marked sample app drawer. Preview does not send commands or pretend to be a connected TV.

## Connect to a TV

This project includes a separate `receiver` APK so the existing launcher doesn't need to be modified. The receiver currently requires **Android TV 13 / API 33 or later** for system-wide directional navigation. Older TVs need a different input backend before they are supported.

```powershell
.\gradlew.bat :receiver:assembleDebug
adb -s <tv-device> install -r receiver\build\outputs\apk\debug\receiver-debug.apk
adb -s <tv-device> shell am start -n com.navadeep.kanso.receiver/.ReceiverActivity
```

1. Put the phone and TV on the same local network.
2. Open Kanso Receiver on the TV and enable **Kanso remote** in accessibility settings. On Android 17, allow local network access as well.
3. Open **Connect** on the phone, choose the TV, and enter the six-digit code shown on the TV. A manual IP address option is available when discovery is blocked by the router.
4. Apps now contains the TV's real app names and icons. Tapping an app launches it on the TV. Home opens Kanso when it is installed, otherwise the system home screen.

The receiver remains available through its accessibility service when another app is open. No cloud account, analytics, or ADB connection is used for normal operation. Disabling the accessibility service stops the receiver. **Forget phones** revokes all paired credentials.

## Controls

- Swipe the trackpad to move focus; tap to select; hold to request a long press.
- Directional buttons respond on touch-down and repeat while held. Center OK also supports a long press.
- Trackpad and Buttons share a large dark touch surface. Home and Play/pause sit to the left of the large Back button, with a vertical rocker on the right. Tap the rocker label or the top-right control to switch between channel and volume buttons. Hold either end to repeat; release or move outside to stop. Live-TV apps default to channel controls; other apps default to volume.
- Channel actions require receiver protocol 3. This receiver activates explicit, accessible Channel up/down or Next/Previous channel buttons in the foreground TV app. It reports unavailable when the app exposes no matching control. It does not inject arbitrary channel key events, substitute D-pad movement, or control an external set-top box. Validate this against the intended live-TV app before treating channel switching as supported on that TV.
- Video apps get Rewind and Fast forward inside the pad; audio apps get Previous and Next. Utilities keep navigation. Profiles use Android app categories with fallbacks for common media apps. Media commands and long-press behavior depend on the TV app; unsupported seek commands may be ignored by that app.
- Apps expands from the compact Remote/Apps navigation over a blurred, recessed remote. The library retains the large two-column 16:9 artwork with labels below. A successful launch carries the selected artwork toward the active-app badge as the remote returns. Android Back, the close control, and Remote return to the remote. Scroll position is retained.
- Saved TV artwork and dock choices can be browsed with the TV offline. The library says Saved apps and offers Connect; tapping an app while disconnected asks for a connection. Cached artwork never implies a successful launch or live TV state.
- Connections close when the phone app goes into the background and reconnect when it returns. Commands are bounded, ordered, and never replayed after a reconnect.

## Security and protocol

Connections use persistent TLS sockets on port 9876. The receiver's private key stays in Android Keystore. The initial pairing uses a short-lived, TV-displayed code over TLS; the phone then pins the receiver certificate. Initial pairing is trust-on-first-use, intended for a trusted home network. Changed certificates require re-pairing. Tokens are random 256-bit values; the receiver stores their hashes. Pairing is rate-limited and only enabled while the receiver's pairing screen is visible. Backup is disabled for both apps.

mDNS service: `_kanso._tcp.`. Messages are UTF-8, newline-delimited JSON. Responses always contain `ok`; failed requests include `error`.

```json
{"type":"pair","code":"123456"}
{"type":"hello","token":"..."}
{"type":"apps","token":"..."}
{"type":"command","token":"...","key":"right"}
{"type":"launch","token":"...","id":"com.google.android.youtube.tv"}
```

Pair returns `token`. Hello returns `name`, protocol `version: 3`, and `activeApp` (the foreground package ID). Apps returns `apps: [{id, name, icon, banner, category}]`; `icon` is a base64 PNG, `banner` is a base64 WebP or an empty string, and `category` is Android's application category. Supported command keys: `up`, `down`, `left`, `right`, `select`, `long_select`, `back`, `home`, `play_pause`, `rewind`, `forward`, `previous`, `next`, `volume_up`, `volume_down`, `volume_mute`, `channel_up`, `channel_down`. The phone retains basic controls with a version 1 receiver, hides the added transport commands, and gives an update notice for channel/mute requests on receivers older than version 3.

## Verify

```powershell
.\gradlew.bat :app:testPreviewUnitTest :app:lintDebug :receiver:lintDebug
.\gradlew.bat :app:connectedPreviewAndroidTest
```

Unit tests cover accumulated trackpad travel, diagonal intent, rapid swipes, and reset behavior. The device test covers mode changes, offline connection, preview, repeated app-panel transitions, and app tap feedback. Additional TV models and long-running OEM background behavior remain untested.

The instrumentation test uses the separate `.preview` application ID, preserving the installed companion's pairing and favorites. It also exercises removing/re-adding dock apps, filtering the library, launch-to-remote transitions, Home reset, and video/audio control profiles. Unit tests cover app order, stale packages, empty docks, moves at boundaries, and remote profiles. `tools/receiver_probe.py --paired-phone <serial> --launch-app com.google.android.youtube.tv` verifies authenticated artwork delivery and foreground tracking through launch and Home. Run this while the paired phone still has a debuggable build; the optimized release intentionally disables `run-as`. The Bravia's Settings app can redirect through account verification, so use a regular media app for this check.

The optimized companion build was installed and visually checked on the Edge 30 with all 35 TV apps. The library scroll/restore check and YouTube launch-to-remote flow passed, preserving the user's dock. A short release-build scroll sample reported 153 frames, 6 Android-classified janky frames, and no missed vsyncs; this is a spot check, not a sustained performance benchmark. The 11 unit tests, phone interaction test, companion lint, receiver lint, and release build passed.

The subsequent reference-based redesign removes the TV illustration, uses the dark remote composition with a large Back control, and restores the larger app banners. Its two phone instrumentation tests and 11 unit tests pass, as do companion lint and the optimized release build. The updated release was installed on the Edge 30; real app artwork, control layout, library scrolling, and retained scroll position were checked. TV navigation was not exercised during this UI pass. Receiver protocol 3 is built but still needs installation and live-TV validation for channel and mute controls.

Verified on a Motorola Edge 30 and a Sony Bravia VU31 running Android 14: LAN discovery, pairing, fetching installed app icons, Home, trackpad focus movement, and launching Crunchyroll from the phone. The original `my-toolkit-tv` launcher is unchanged.

## License

MIT. See [LICENSE](LICENSE).
