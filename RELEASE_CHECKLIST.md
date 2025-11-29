# Snap Swipe – Release Prep Checklist

- [ ] Set release version in `app/build.gradle` (`versionCode`, `versionName`) per `VERSIONING.md`.
- [ ] Configure signing for release (Play App Signing or local keystore) in Gradle or Android Studio.
- [ ] Build and test release APK/AAB: `./gradlew assembleRelease` and smoke test on device (permissions, keep/delete/queue delete, share, settings).
- [ ] Verify shrink/obfuscate: ensure images/loaders work and no crashes with R8 enabled.
- [ ] Confirm manifest flags: `allowBackup=false`, correct permissions (`READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` maxSdk32), `debuggable` off.
- [ ] Prepare Play listing assets: screenshots (permissions, swipe, share, settings), feature graphic, icon.
- [ ] Fill Data Safety: accesses local photos only; no collection/analytics if unchanged. Link Privacy Policy/Terms in Play Console.
- [ ] Final logs/telemetry: no sensitive logs in release; add crash/analytics only with consent.
