# Voice Gesture Automation

Android app (Kotlin) that links a spoken trigger word to a custom on-screen gesture you draw
yourself. When the word is heard, the app replays your recorded gesture automatically.

## How it works

- **Add screen** (`AddAutomationActivity`) — enter the word to listen for and draw the gesture
  in the canvas (`GestureDrawingView`). Multi-finger gestures are supported; each finger's path
  and timing is recorded.
- Automations are stored locally with Room (`Automation` entity: trigger word + serialized
  gesture strokes).
- **`VoiceListenerService`** is a foreground service (type `microphone`) that continuously runs
  `SpeechRecognizer`, restarting the recognition session on every result/error so listening never
  really stops. Because it's a proper foreground service with a persistent notification, Android
  keeps allowing microphone access even while the screen is off/locked — this is what makes
  detecting the word while locked possible. A partial wake lock keeps the CPU alive for the same
  reason.
- **`GestureAccessibilityService`** is an `AccessibilityService` that replays the recorded
  strokes with `dispatchGesture()` once a trigger word matches.

## Required setup on-device

1. Grant the microphone (and, on Android 13+, notification) permission from the app's main
   screen switch.
2. Enable the Accessibility Service once from Settings — Android does not allow apps to turn on
   accessibility services programmatically, so the app links out to
   `Settings.ACTION_ACCESSIBILITY_SETTINGS` for this.

## Known platform limitation

Voice recognition keeps running while the screen is locked, but Android restricts
`AccessibilityService.dispatchGesture()` while a secure keyguard is actively showing, so the
gesture reliably replays as soon as the device is unlocked rather than through the lock screen
itself. This is a platform security boundary, not a bug in the matching/listening logic.

## Project structure

```
app/src/main/java/com/automation/voicegesture/
  MainActivity.kt              automation list, permission + service toggles
  AddAutomationActivity.kt     word input + gesture recording
  data/                        Room entity/DAO/DB + gesture (de)serialization
  view/GestureDrawingView.kt   multi-touch gesture capture canvas
  service/VoiceListenerService.kt        continuous speech recognition
  service/GestureAccessibilityService.kt gesture replay
  receiver/BootReceiver.kt     resumes listening after reboot
```

## Building

Standard Gradle Android project (AGP 8.2.2, Kotlin 1.9.22, compileSdk/targetSdk 34, minSdk 26).
Open in Android Studio (Koala+) or run `./gradlew assembleDebug` with the Android SDK installed.
