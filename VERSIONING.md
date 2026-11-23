# Snap Swipe – Versioning

This document defines how we track and bump versions for Snap Swipe. The goal is a simple, readable mapping while keeping Play Store `versionCode` monotonic.

## Versioning scheme
- `versionName`: Semantic version `MAJOR.MINOR.PATCH`.
  - MAJOR: Breaking UX/behavior shifts or impactful platform changes.
  - MINOR: New features or notable improvements.
  - PATCH: Bug fixes and small polish.
- Default `versionCode` scheme: Encode as two digits per segment: `MMmmpp` → `MAJOR * 10000 + MINOR * 100 + PATCH`.
  - Examples:
    - `1.2.1`  → `010201` (integer `10201`)
    - `2.3.12` → `020312` (integer `20312`)
    - `1.12.2` → `011202` (integer `11202`)
  - Keep each segment 0–99; bump MAJOR/MINOR/PATCH to ensure the resulting integer is strictly greater than any build already published.

## How to bump
1. Pick the next `versionName` (SemVer).
2. Use the release build number if one is specified. Otherwise use the default formula if it produces an unused number above the latest Play build. If it does not, choose the next unused higher number. Version 1.4.0 uses the explicit build number 14000.
3. Update `app/build.gradle` `defaultConfig.versionName` and `defaultConfig.versionCode`.
4. Update release notes and record the build in the log below.

## Version log
Track release builds here. Historical codes retain their original zero padding.

| versionName | versionCode | Date | Notes |
|-------------|-------------|------|-------|
| 1.4.2       | 14002       | -    | Bundles Indonesian, Turkish, Bengali, and Arabic translations. |
| 1.4.0       | 14000       | -    | Adds review resume and the photo timeline, fixes deletion and photo access, and targets Android 16 (API 36). |
| 1.3.1       | 010301      | -    | Prepared Android 16 (API 36) compliance update for Play build 10300. |
| 1.3.0       | 010300      | -    | Settings UI refresh, theme selector (light/dark/system), and slimmer floating header. |
| 1.2.1       | 010201      | -    | Targets Android 16 (API 36) for Play compliance and fixes edge-to-edge padding on the main screen. |
| 1.2.0       | 010200      | -    | Adds scroll interaction mode, expands language support, and shows a one-time What&apos;s New prompt. |
| 1.1.0       | 010100      | -    | Added multi-language support. |
| 1.0.0       | 010000      | -    | First swipe app release. |
| 0.1.0       | 000100      | -    | Alpha rough version of the app. |
