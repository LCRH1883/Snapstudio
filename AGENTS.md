# AGENTS – Snap Swipe

## Project

- Name: Snap Swipe
- Type: Native Android app
- Language: Kotlin
- UI: Jetpack Compose
- Purpose: Help users clean up photos by swiping one photo at a time, deleting or keeping them and optionally sharing.

## Agents

- Human developer  
  - Owns product decisions, runs the app, tests on device/emulator, and reviews changes.

- Codex (this agent)  
  - Main implementation agent for this repository.  
  - Writes and updates Kotlin/Compose code, Gradle configuration, and documentation.  
  - Follows the build plan in BUILD_PLAN.md step-by-step.  
  - After each major step, updates BUILD_PLAN.md and the task log below.

- Claude (code)  
  - Secondary coding agent, focused on code review, refactoring, debugging, and tests.  
  - Reads AGENTS.md and BUILD_PLAN.md before working.  
  - Keeps changes aligned with the architecture and behavior defined here.

## Architecture Summary

- Single-activity Jetpack Compose app (no XML layouts for primary UI).
- Kotlin with coroutines for background work.
- Jetpack libraries:
  - Compose UI + Material3
  - Activity Compose
  - Navigation Compose
  - DataStore (Preferences) for settings
  - Coil (Compose integration).
  - Photo access: Android MediaStore.
  - Permissions: runtime permissions for reading images.

## Workflow Rules

1. Before making non-trivial changes, Codex and Claude must:
   - Read this file (AGENTS.md).
   - Read BUILD_PLAN.md to understand the current phase and remaining steps.

2. When implementing or modifying features:
   - Keep the app behavior consistent with the “Core app behavior” described in BUILD_PLAN.md.
   - Prefer small, incremental commits/changes that can be built and tested after each step.

3. After completing a major step in BUILD_PLAN.md:
   - Update BUILD_PLAN.md to mark the step as complete and add any follow-up notes.
   - Append a new row to the Task Log below summarizing what changed.

4. If architecture or requirements change:
   - Update AGENTS.md (this file) and BUILD_PLAN.md to match the new reality.

## Task Log (append new rows; do not rewrite history)

| Step ID | Date (optional) | Agent  | Summary of work                           |
|--------|------------------|--------|-------------------------------------------|
| 0.1    | 2024-11-24       | Codex  | Added AGENTS.md, claude.md, BUILD_PLAN.md per setup instructions. |
| 0.2    | 2024-11-24       | Codex  | Scaffolded Compose-based Android project (minSdk 26, targetSdk 34, pkg com.example.snapswipe). |
| 0.3    | 2024-11-24       | Codex  | Configured Compose build features/options and dependencies in app Gradle config. |
| 0.4    | 2024-11-24       | Codex  | Added SnapSwipeApp navigation scaffold with placeholder screens (permissions, main, settings) and wired MainActivity. |
| 1.1-1.3| 2024-11-24       | Codex  | Implemented permissions UI, runtime request flow, and navigation gate to block main screen until access is granted. |
| 2.1    | 2025-11-24       | Codex  | Added PhotoItem data model (id, uri, dateTaken) in the data layer. |
| 2.2    | 2025-11-24       | Codex  | Added SortOrder enum and MediaStore-backed PhotoRepository with loadPhotos logging count. |
| 2.3    | 2025-11-24       | Codex  | Added SnapSwipeViewModel wired to PhotoRepository and updated main screen to show index/count with keep/delete advancing. |
| 3.1    | 2025-11-24       | Codex  | Added SortOrderPreferences DataStore helper and debug UI toggle for sort order. |
| 3.2    | 2025-11-24       | Codex  | Implemented SettingsScreen with sort order radio options bound to DataStore and about text. |
| 3.3    | 2025-11-24       | Codex  | Wired sort-order preference to reload photos via ViewModel when changed; navigation uses new Settings screen. |
| 4.1    | 2025-11-24       | Codex  | Built MainSwipeScreen with AsyncImage, top bar + settings icon, gesture hint, and keep/delete controls; main route now uses it. |
| 4.2    | 2025-11-24       | Codex  | Added swipe gesture handling (left=keep, right-to-left=delete, up=share placeholder) on MainSwipeScreen. |
| 4.3    | 2025-11-24       | Codex  | Added empty and end-of-run states with restart/refresh buttons; ViewModel tracks end-of-list and restart/reload actions. |
| 5.1-5.2| 2025-11-24       | Codex  | Implemented delete via MediaStore (intent sender on R+), wired trashCurrent to remove items; keepCurrent now simply advances. |
| icon   | 2025-11-24       | Codex  | Set launcher icon to provided snap swipe.png image via adaptive icon foreground. |
| 5.3    | 2025-11-24       | Codex  | Processed photos removed from in-memory list (keep/delete), undo restores last, end-of-run handled without re-showing processed items. |
| 6.1    | 2025-11-24       | Codex  | Added share ModalBottomSheet on upward swipe with share/cancel actions. |
| 6.2    | 2025-11-24       | Codex  | Implemented share intent via chooser using photo URI with read permission grant. |
| 7.1    | 2025-11-24       | Codex  | Updated theme palette/typography and main overlay styling for a more refined look and better dark-mode support. |
| icon v2| 2025-11-24       | Codex  | Updated launcher foreground icon to new img/icon asset. |
| icon v3| 2025-11-24       | Codex  | Copied new icon to all mipmap density folders to improve clarity/scaling. |
| icon v4| 2025-11-24       | Codex  | Re-copied new icon into mipmap-anydpi-v26 foreground to ensure adaptive icon uses the right asset. |
| pkg    | 2025-11-24       | Codex  | Changed package/applicationId to com.snapswipe.app (was com.example.snapswipe). |
| api35  | 2025-11-24       | Codex  | Raised compile/target SDK to 35 and noted requirement update in BUILD_PLAN.md. |
| 7.2    | 2025-11-24       | Codex  | Added MediaStore error logging, permission-denied messaging, and delete failure handling per build plan step 7.2. |
| 7.3    | 2025-11-24       | Codex  | Added Robolectric/coroutines unit tests for SnapSwipeViewModel and introduced PhotoDataSource abstraction for testing. |
| 7.4    | 2025-11-24       | Codex  | Marked final verification checklist; automated tests pass, device flows to be validated on emulator/device. |
| 8.1    | 2025-11-28       | Codex  | Added locale config and Settings language picker applying AppCompatDelegate app locales. |
| 8.2    | 2025-11-28       | Codex  | Enabled autoStoreLocales service and set fallback locale to ensure device language is chosen by default. |
| 9.1    | 2025-11-28       | Codex  | Added “Scroll and delete” interaction mode with gesture re-mapping, animations, and settings toggle. |
| 9.2    | 2025-11-28       | Codex  | Removed in-app language picker; Settings now reflects device language only. |
| 9.3    | 2025-11-28       | Codex  | Removed language section from Settings entirely; localization follows device/app language. |
| 9.4    | 2025-11-28       | Codex  | Added locales and strings for Japanese, Vietnamese, Thai, and Hindi; updated locale config and docs. |
| 9.5    | 2025-11-28       | Codex  | Added Portuguese, Italian, Dutch, and Ukrainian locales/strings; updated locale config and docs. |
| ver1.2.0 |                  | Codex  | Switched to zero-padded MMmmpp versionCode scheme, set 1.2.0 / 010200, added VERSIONING.md + VERSION_HISTORY.md. |
| ver1.2.0a |                  | Codex  | Added one-time “What’s New” popup keyed to version, updated strings and release notes for scroll mode + languages. |
