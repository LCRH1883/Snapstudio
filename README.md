# Snap Swipe

Snap Swipe is a Jetpack Compose Android app that helps you clean up your photos one by one: swipe to keep or delete, queue deletes for batch confirmation, and share with an upward swipe. It supports multiple languages (English, Korean, French, Spanish, German, Simplified Chinese).

## Key features
- Swipe left to delete (immediate or queued), right to keep, up to share.
- Optional “Scroll and delete” mode: left to delete, right to share, vertical swipes move to next/previous photo with slide animation.
- Delete modes: immediate (per-photo confirmation when required) or queued (confirm a batch once).
- Undo/back to step through previous actions; home jumps to start/end based on sort order.
- Sort order: newest-to-oldest or oldest-to-newest.
- Share via system chooser with read permission granted.
- End-of-run and empty-state handling with restart/refresh.
- Localized UI in 18 languages, including Indonesian, Turkish, Bengali, and Arabic.
- Permission rationale explains local-only photo access; nothing is uploaded.

## Build & run
Requirements: Android Studio (Giraffe+), JDK 17, Android SDK 35.

### Debug build
```
./gradlew assembleDebug
```
Install on device/emulator and run. First launch will prompt for photo access.

### Release build (Play-ready)
1) Set signing config (keystore or Play App Signing).  
2) Build bundle:
```
./gradlew bundleRelease
```
Artifact: `app/build/outputs/bundle/release/app-release.aab`.
If you prefer APK for testing:
```
./gradlew assembleRelease
```
Artifact: `app/build/outputs/apk/release/app-release.apk`.

### Release checklist (summary)
- Version 1.4.2, build 14002. See [versioning](VERSIONING.md).
- Shrink/obfuscate enabled for release; allowBackup=false.
- Permissions: READ_MEDIA_IMAGES (and READ_EXTERNAL_STORAGE maxSdk32).
- Smoke-test release build on device: permissions, swipes, delete modes, share, settings, numbering, undo/back, home.
- Play listing assets: screenshots (permission, swipe, queue delete, share, settings), feature graphic, icon.
See `RELEASE_CHECKLIST.md` for full list.

## Localization
- Keep English source strings in `app/src/main/res/values/strings.xml` and bundled translations in the locale-specific `values-*` folders. The supported locale list is in `app/src/main/res/xml/locales_config.xml`.
- Compose uses `stringResource` to match the Android app or device language, with English as the fallback.
- Android 13 and later offer **Settings → Apps → Snap Swipe → Language**. Earlier Android versions use the device language. The release bundle installs every supported language so switching works offline.
- To ship the bundled translations, disable automatic app-string translation for their languages in Play Console. Google Play overrides bundled strings for languages it manages.

## Permissions and privacy
- Requests photo access (READ_MEDIA_IMAGES; legacy READ_EXTERNAL_STORAGE on SDK ≤ 32).
- Photos are read locally for keep/delete/share; the app does not upload or store them remotely.
- Data Safety: declare local photo access only; no analytics by default.

## Testing
- Unit tests (Robolectric + coroutines): `./gradlew :app:testDebugUnitTest`.
- Please also run on a device/emulator to validate MediaStore delete/permission flows and release (R8) build.
