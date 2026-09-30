# Chip test

A minimal Android app that shows promoted status bar chips (Live Updates) leaking private notification
content while the lock screen is occluded.

It posts one promoted ongoing notification with visibility `PRIVATE`. The private version's chip text is
`4827`; its public version's is `PUB`. On a locked phone set to hide sensitive notification content, the
status bar must never show `4827`.

## Build

Needs the Android SDK (build-tools 36.0.0, platform android-36) and a JDK 17 or newer:

```
./build.sh
adb install chip-test.apk
```

`build.sh` uses the SDK tools directly (no Gradle) and signs with a local test key it creates in `build/`. It
takes `javac` and `keytool` from `$JAVA_HOME/bin` when `JAVA_HOME` is set (for example Android Studio's bundled JDK).

## Reproduce

1. Open "Chip leak test", allow notifications and tap **Post Live Update**. The status text should read
   `can post promoted: true` and `promotable: true`, and a chip with `4827` shows in the status bar.
2. Settings › Notifications: hide sensitive content on the lock screen.
3. Lock the phone and wake the screen: no chip shows, as expected.
4. Without unlocking, open the emergency dialer (swipe up, **Emergency call**). It shows over the lock
   screen, which is now occluded but still locked.
5. Look at the status bar: the chip shows `4827`, the private content, although the phone is locked.

Seen on GrapheneOS (tag 2026091000, Android 17) on a Fairphone 6, with SystemUI's chip code unchanged
from AOSP.

## Cause

`StatusBarVisibilityInteractor` allows the home status bar whenever the keyguard is occluded, and
`NotifChipsViewModel.toPrunedModel()` always uses `promotedContent.privateVersion`, on the assumption
that chips are never shown while the device is locked.

## Fix

Proposed fix for GrapheneOS's `platform_frameworks_base`:
<https://github.com/abhinav-jhanwar/platform_frameworks_base/tree/chip_leak>. A chip whose notification the
lock screen would redact shows only the public version's short text, or just the icon. Notifications the
lock screen hides already give no chip.

## License

Apache-2.0 (see the SPDX line in the source).
