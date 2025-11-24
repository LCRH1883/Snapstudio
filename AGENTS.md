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
