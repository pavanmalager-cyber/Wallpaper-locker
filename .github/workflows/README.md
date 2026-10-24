# Wallpaper Lock (Android)

A starter native Android app project for selecting a wallpaper, applying it, and starting a timer with no in-app cancel/unlock control.

## Important limitation
This is an app-level commitment lock only. Android does not let an ordinary third-party app reliably prevent the user from changing the wallpaper through Settings, another app, uninstalling the app, clearing its data, or changing device time. Strong system-wide enforcement generally requires managed-device/device-owner provisioning and is not guaranteed on a regular personal phone.

## Build an APK using GitHub Actions from a phone
1. Create a GitHub repository named `WallpaperLockAndroid`.
2. Upload the contents of this folder to the repository (not the enclosing folder).
3. Open the repository's **Actions** tab and allow workflows if prompted.
4. Run **Build Wallpaper Lock APK** using **Run workflow**, or push a commit to `main`.
5. Open the completed workflow run, scroll to **Artifacts**, and download `WallpaperLock-debug-apk`.
6. Extract the ZIP and install `app-debug.apk`. Android may ask you to allow installs from that browser/file manager.

## Local build
Requires Android SDK (API 35) and Gradle 8.7+ with Android Gradle Plugin 8.6.1.

## Current scope
- Image picker
- Apply selected image to home screen and attempt lock screen
- 1-hour, 24-hour, and 7-day presets
- Persistent end timestamp and countdown
- No in-app early unlock button

This is a starter implementation and should be tested on the target OnePlus model before relying on it.
