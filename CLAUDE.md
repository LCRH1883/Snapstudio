# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Snapstudio is an Android application built with Gradle. The project is in its initial setup phase - the core Android project structure (app module, source directories) has not yet been created.

## Project Structure

This is a standard Android/Gradle project following the conventional layout:

- **App code**: `app/src/main/` - Kotlin/Java sources under `java/`, resources under `res/`, shared assets in `assets/`
- **Unit tests**: `app/src/test/` - JVM unit tests (JUnit, Mockito/MockK)
- **Instrumented tests**: `app/src/androidTest/` - UI/integration tests (Espresso/Compose test APIs)
- **Build configuration**:
  - `settings.gradle` - project-level settings
  - Root `build.gradle` (or `.kts`) - project-level build config
  - `app/build.gradle` - app module build config
  - `local.properties` - local environment config (never commit this)

## Build and Development Commands

```bash
# Build and assemble
./gradlew assembleDebug              # Compile and package debug APK
./gradlew assembleRelease            # Compile and package release APK

# Testing
./gradlew testDebugUnitTest          # Run JVM unit tests
./gradlew connectedDebugAndroidTest  # Run instrumented tests on device/emulator

# Code quality
./gradlew lint                       # Run Android lint checks (fix violations before PRs)

# Development
# After changing Gradle scripts, use Android Studio's "Sync Project with Gradle Files"
```

## Coding Conventions

**Language and Style**:
- Kotlin is preferred over Java
- 4-space indentation, trailing commas where allowed
- Use idiomatic Kotlin null-safety patterns

**Naming**:
- Classes/objects: `PascalCase`
- Functions/properties: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Package names: lowercase with no underscores, matching directory structure

**Architecture**:
- Favor small, single-responsibility classes
- Avoid God-objects/activities
- Use Android Studio's "Code > Reformat Code" and optimize imports before commits

## Testing Requirements

**Unit Tests**:
- Use JUnit with Mockito or MockK for mocking
- Test naming: `FeatureThingTest`, methods named `featureThingShould...`
- Always add regression coverage for bugs before marking them fixed
- New features require at least one happy-path test and one failure-path test

**Instrumented Tests**:
- Use Espresso for View-based UI or Compose test APIs for Jetpack Compose
- Rotate/wipe test devices when encountering flakiness in `connected*` tasks

## Commit Guidelines

- Use imperative mood, concise scope (e.g., `Add login validation`, `Fix crash on rotation`)
- Prefer Conventional Commits format when type is clear: `feat:`, `fix:`, `chore:`
- Keep commits focused - don't bundle unrelated changes with formatting
- PRs should describe the change, testing performed, and any UI notes/screenshots for visual changes
- Link issues/tickets when applicable, call out risky migrations or follow-ups explicitly

## Security

- Never commit secrets, keystores, or `local.properties`
- Use environment variables or encrypted storage for signing configurations
- `local.properties` contains local SDK path and must remain untracked
