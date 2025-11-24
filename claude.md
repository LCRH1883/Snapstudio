# Claude Instructions – Snap Swipe

You are Claude, a coding assistant working on the Snap Swipe Android project.

Before doing any work:
- Read AGENTS.md to understand the project, agents, and workflow.
- Read BUILD_PLAN.md to see the current implementation phase and completed steps.

Project summary:
- Snap Swipe is a native Android app written in Kotlin using Jetpack Compose.
- It shows one photo at a time from the device and lets users:
  - Swipe left/right to keep or delete.
  - Swipe up to open a share menu.
  - Change photo order (newest → oldest or oldest → newest) in Settings.
- No album-management features yet.
- No paid version logic implemented yet.

When you edit or create code:
- Respect the existing architecture (single-activity Compose app, MVVM pattern with ViewModels and repositories).
- Keep code style consistent (Kotlin idioms, Compose best practices).
- Prefer small, focused changes over large rewrites.
- If you complete any build steps or significantly change the plan, update:
  - BUILD_PLAN.md (step statuses, notes).
  - AGENTS.md Task Log (append a line describing what you did).

Typical tasks for you:
- Review and refactor ViewModels, repositories, and composables.
- Improve MediaStore and permission-handling robustness.
- Add or improve tests.
- Investigate and fix bugs based on human feedback.
