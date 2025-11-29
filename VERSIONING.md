# Snap Swipe – Versioning

This document defines how we track and bump versions for Snap Swipe. The goal is a simple, readable mapping while keeping Play Store `versionCode` monotonic.

## Versioning scheme
- `versionName`: Semantic version `MAJOR.MINOR.PATCH`.
  - MAJOR: Breaking UX/behavior shifts or impactful platform changes.
  - MINOR: New features or notable improvements.
  - PATCH: Bug fixes and small polish.
- `versionCode`: Encode as two digits per segment: `MMmmpp` → `MAJOR * 10000 + MINOR * 100 + PATCH`.
  - Examples:
    - `1.2.1`  → `010201` (integer `10201`)
    - `2.3.12` → `020312` (integer `20312`)
    - `1.12.2` → `011202` (integer `11202`)
  - Keep each segment 0–99; bump MAJOR/MINOR/PATCH to ensure the resulting integer is strictly greater than any build already published.

## How to bump
1. Pick the next `versionName` (SemVer).
2. Compute `versionCode = (MAJOR * 10000) + (MINOR * 100) + PATCH` (write it zero-padded as `MMmmpp` in docs for clarity).
3. Update `app/build.gradle` `defaultConfig.versionName` and `defaultConfig.versionCode`.
4. Update release notes and record the build in the log below.

## Version log
Track each published build here (versionCode shown as zero-padded `MMmmpp`):

| versionName | versionCode | Date | Notes |
|-------------|-------------|------|-------|
| 1.3.0       | 010300      | -    | Settings UI refresh, theme selector (light/dark/system), and slimmer floating header. |
| 1.2.0       | 010200      | -    | Adds scroll interaction mode, expands language support, and shows a one-time What&apos;s New prompt. |
| 1.1.0       | 010100      | -    | Added multi-language support. |
| 1.0.0       | 010000      | -    | First swipe app release. |
| 0.1.0       | 000100      | -    | Alpha rough version of the app. |
