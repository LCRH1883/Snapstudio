# Snap Swipe – Build Plan

This file tracks the implementation of the Snap Swipe Android app.

For each step:
- Codex should implement the described changes.
- After implementation, run a build and (where applicable) run the app to manually test.
- When a step is done, mark its checkbox as `[x]` and add any notes.
- Important: keep this file in sync with AGENTS.md and CLAUDE.md.

------------------------------------------------------------
PHASE 0 – Documentation & Project Setup
------------------------------------------------------------

[x] 0.1 – Create agent docs
    - Create AGENTS.md with the contents from the initial instructions.
    - Create claude.md with the contents from the initial instructions.
    - Ensure both files are committed/available in the repository.
    - Manual check: AGENTS.md and claude.md exist at repo root and render correctly as Markdown.

[x] 0.2 – Create Android Studio project for Snap Swipe
    - Project name: Snap Swipe.
    - Package name: e.g. com.example.snapswipe (or equivalent).
    - Language: Kotlin.
    - Min SDK: 26.
    - Target/compile SDK: latest stable available.
    - Template: a Compose-based template (Empty Compose Activity, or Empty Activity with Jetpack Compose enabled).
    - Manual check: Project builds successfully; main activity runs and shows the default Compose preview content.
    - Notes: Updated compile/target SDK to API 35 to meet Play requirements.

[x] 0.3 – Configure Jetpack Compose dependencies
    - In the app module build.gradle(.kts), ensure:
      - Compose is enabled in buildFeatures.
      - Compose compiler extension version is set.
      - Dependencies include:
        - Compose BOM.
        - androidx.compose.ui:ui
        - androidx.compose.material3:material3
        - androidx.activity:activity-compose
        - androidx.navigation:navigation-compose
        - Coil Compose for image loading.
        - DataStore Preferences for settings.
    - Manual check: Sync/Build succeeds; basic composable screen compiles.

[x] 0.4 – Create initial app scaffold
    - Create a composable SnapSwipeApp() that:
      - Sets up Material3 theme.
      - Provides a NavHost for navigation.
      - For now, defines at least three routes:
        - "permissions" – permissions/onboarding screen.
        - "main" – main swipe screen (currently placeholder).
        - "settings" – settings screen (currently placeholder).
    - Set MainActivity to call setContent { SnapSwipeApp() }.
    - Manual check: App builds and runs; you can navigate between simple placeholder screens using a debug-only UI (e.g. buttons).

------------------------------------------------------------
PHASE 1 – Permissions & Onboarding
------------------------------------------------------------

[x] 1.1 – Implement permissions screen UI
    - Create a composable PermissionsScreen:
      - Show app name and a short explanation: Snap Swipe needs access to photos to help you clean them up.
      - Show a primary button: “Grant photo access”.
      - Optional secondary button: “Continue without access” that shows an informative message or exits.
    - Manual check: PermissionsScreen displays correctly when the app starts.

[x] 1.2 – Implement runtime permission request logic
    - Use rememberLauncherForActivityResult for requesting:
      - READ_MEDIA_IMAGES on Android 13+.
      - READ_EXTERNAL_STORAGE on older versions (if needed).
    - When the user taps “Grant photo access”:
      - Trigger permission request.
    - On result:
      - If granted: navigate to "main" screen.
      - If denied: show appropriate error message and allow retry.
    - Manual check: On an emulator/device, verify permission dialog appears and navigation behavior is correct.

[x] 1.3 – Permission gate before main screen
    - Ensure navigation logic checks permissions before showing the main swipe UI.
    - If the permission is not granted at app start:
      - Start on "permissions" route.
    - If permission is granted:
      - Start on "main" route.
    - Manual check: Reinstall app, test both flows (grant vs deny).

------------------------------------------------------------
PHASE 2 – Photo Data Layer (MediaStore)
------------------------------------------------------------

[x] 2.1 – Define photo model
    - Create a data class PhotoItem with at least:
      - id: Long (MediaStore ID).
      - uri: Uri.
      - dateTaken: Long? (or similar).
    - Manual check: Data class compiles and is used only within the data layer initially.
    - Notes: Added PhotoItem data class with id, uri, and dateTaken fields.

[x] 2.2 – Implement MediaStore photo repository
    - Create a repository, e.g. PhotoRepository, responsible for:
      - Querying MediaStore for images from EXTERNAL_CONTENT_URI.
      - Mapping cursor results to PhotoItem objects.
      - Respecting a sort order: NEWEST_FIRST or OLDEST_FIRST.
    - Define an enum SortOrder { NEWEST_FIRST, OLDEST_FIRST }.
    - Implement a method:
      - suspend fun loadPhotos(sortOrder: SortOrder): List<PhotoItem>
    - Handle edge cases (no photos, null dates).
    - Manual check: Temporarily log or display the count of loaded photos to verify it works.
    - Notes: Added SortOrder enum and PhotoRepository querying MediaStore on Dispatchers.IO, logging loaded count and handling null/zero dates.

[x] 2.3 – Wire repository into a ViewModel
    - Create a SnapSwipeViewModel (or MainViewModel) that:
      - Holds the list of PhotoItem loaded from the repository.
      - Exposes the current index and current photo.
      - Provides functions: nextPhoto(), keepCurrent(), trashCurrent() (initially only changing index; deletion to be implemented later).
    - Manual check: In a debug placeholder main screen, show:
      - Current photo index and total count.
      - Buttons for “Keep” and “Delete” that just advance to next photo for now.
    - Notes: Added SnapSwipeViewModel + factory, loads on init, exposes simple state and actions; main screen now shows index/count with keep/delete advancing to next.

------------------------------------------------------------
PHASE 3 – Settings (Sort Order) with DataStore
------------------------------------------------------------

[x] 3.1 – Set up DataStore Preferences
    - Add DataStore initialization in a suitable place (e.g. a singleton or repository).
    - Define a preference key for photo sort order (string or int).
    - Provide functions to:
      - Read current sort order as a Flow<SortOrder>.
      - Persist updated sort order.
    - Manual check: Create a temporary UI or log to confirm the sort order can be read and updated.
    - Notes: Added SortOrderPreferences with DataStore delegate, Flow of SortOrder, and setter; main debug UI shows and toggles stored sort order.

[x] 3.2 – Implement Settings screen UI
    - Create SettingsScreen composable with:
      - A simple top app bar labeled “Settings”.
      - A section for “Photo review order” with:
        - Radio buttons or a toggle for:
          - “Newest to oldest” (default).
          - “Oldest to newest”.
      - A brief “About Snap Swipe” text (1–2 sentences) and a placeholder version string.
    - Manual check: SettingsScreen renders correctly and reads the current sort order from DataStore.
    - Notes: Added SettingsScreen with top app bar, radio options bound to DataStore sort order, and about/placeholder version text.

[x] 3.3 – Connect Settings to navigation and data
    - Add navigation from the main screen to Settings (e.g., top app bar icon).
    - When the sort order changes:
      - Persist it via DataStore.
      - Trigger reload of photos in the ViewModel with the new order.
    - Manual check: Changing the sort order in Settings and returning to the main screen should show photos in the correct order (verify by comparing to the device gallery).
    - Notes: Main screen collects sort-order preferences and calls ViewModel.loadPhotos on changes; SettingsScreen writes to DataStore via radio options.

------------------------------------------------------------
PHASE 4 – Main Swipe UI with Gestures
------------------------------------------------------------

[x] 4.1 – Basic main screen layout
    - Create MainSwipeScreen composable that:
      - Displays the current photo using Coil’s AsyncImage (or equivalent).
      - Shows optional overlay UI:
      - A simple top bar with app title and Settings icon.
      - A small hint text for gestures (e.g., “Swipe left to delete, right to keep, up to share”).
    - Manual check: With fake or debug images, confirm the layout works in both portrait and (optionally) landscape.
    - Notes: Added MainSwipeScreen with AsyncImage full-bleed photo, top bar + settings icon, gesture hint, and keep/delete buttons; wired main route to it.

[x] 4.2 – Implement swipe gesture handling
    - Use Compose gesture APIs (e.g. pointerInput, swipeable, or draggable) to detect:
      - Horizontal swipes: distinguish left vs right.
      - Vertical swipe upwards to trigger the bottom menu.
    - Map gestures:
      - Left swipe (right-to-left): call ViewModel.keepCurrent() or trashCurrent() depending on your mapping; for this project:
        - Right-to-left = delete/trash.
        - Left-to-right = keep.
      - Upwards swipe: open share menu (bottom sheet).
    - Make sure:
      - After a keep or delete action, the main screen advances to the next photo (or shows a suitable “no more photos” state).
    - Manual check: On-device tests swiping left, right, and up to confirm correct callbacks.
    - Notes: Added pointerInput drag detection on MainSwipeScreen mapping right-to-left to delete, left-to-right to keep, and upward to share callback (placeholder for share flow).

[x] 4.3 – “No photos” and “end of list” states
    - When there are no photos at all:
      - Show a friendly empty state: “No photos found. Take some photos and come back!”.
    - When the user reaches the end of the list:
      - Show a message like: “You’ve reviewed all photos in this run.”
      - Allow restarting or going back to settings.
    - Manual check: Use a test device/emulator with few or no photos to verify these paths.
    - Notes: Main screen now shows empty state with refresh/settings buttons and end-of-run state with restart/settings actions; ViewModel tracks end-of-list and restart/reload hooks.

------------------------------------------------------------
PHASE 5 – Delete / Trash and Keep Behavior
------------------------------------------------------------

[x] 5.1 – Implement delete/trash operation
    - In the repository, implement a function to delete a given PhotoItem:
      - For newer Android versions, use the recommended MediaStore APIs to request deletion, which may show a system confirmation dialog.
      - For older versions, use ContentResolver.delete() on the photo URI.
    - In the ViewModel, wire trashCurrent():
      - Call the repository delete method for the current photo.
      - On success, move to the next photo.
      - Handle errors (e.g., log or show a simple message).
    - Manual check: On a test device (with non-critical photos), swipe to delete and verify the photo is actually removed from the system gallery.
    - Notes: PhotoRepository deletePhoto handles R+ delete requests via intent sender; ViewModel launches delete intent when required, removes item on success, and shows errors otherwise.

[x] 5.2 – Implement keep behavior
    - For keepCurrent(), do not modify the photo at all.
    - Simply advance to the next photo.
    - Manual check: After swiping to keep, verify the photo still appears in the system gallery and the app shows the next one.
[x] 5.3 – Avoid re-showing processed photos in the same run
    - Optional but recommended:
      - Maintain an in-memory set or simple state that tracks which photos were processed during the current session.
      - Do not show processed photos again until the app is restarted or the list is reloaded intentionally.
    - Manual check: Repeated swiping in the same run should not loop the same photo.
    - Notes: Keep and delete now remove the photo from the in-memory list, track last action for undo, and show end-of-run when all processed.

------------------------------------------------------------
PHASE 6 – Share Bottom Menu
------------------------------------------------------------

[x] 6.1 – Implement bottom sheet UI
    - When the user swipes up:
      - Open a ModalBottomSheet (or similar).
      - Show actions:
        - “Share photo” – triggers system share.
        - “Cancel” – closes the sheet.
    - Manual check: Swiping up on a photo shows and hides the bottom sheet correctly.
    - Notes: Added ModalBottomSheet on upward swipe with share/cancel options; sheet opens from main screen.

[x] 6.2 – Implement system share intent
    - Implement a helper function to share the current photo:
      - Use Intent.ACTION_SEND with the photo’s content URI.
      - Use Intent.createChooser so the user can pick the app (Google Photos, Messages, etc.).
      - Ensure you use a content URI that other apps can access (MediaStore URI should work; use appropriate flags).
    - Manual check: Share a photo and confirm that target apps receive and display it.
    - Notes: Share callback now launches ACTION_SEND chooser with content URI and grants read permission; errors are logged if no handler.

------------------------------------------------------------
PHASE 7 – Polish and Testing
------------------------------------------------------------

[x] 7.1 – Visual polish
    - Improve spacing, typography, and colors using Material3.
    - Ensure dark mode looks reasonable.
    - Add a simple app icon placeholder for Snap Swipe.
    - Manual check: Run on multiple screen sizes to ensure UI looks acceptable.

[x] 7.2 – Basic error handling and logging
    - Add safe error handling for:
      - MediaStore query failures.
      - Permission issues.
      - Delete failures.
    - Use Android logging (e.g., Log.d/e) where helpful.
    - Manual check: Manually trigger error paths where reasonable (e.g., revoke permission) and confirm app doesn’t crash.
    - Notes: Added logging for MediaStore query failures/null cursors, delete attempts, and queued delete batches; ViewModel now surfaces permission-specific errors and delete failures; permission screen shows denial hint.

[x] 7.3 – Simple tests
    - Add unit tests for:
      - ViewModel logic (advancing index, handling keep/delete, reacting to sort order changes).
      - Simple repository tests (where possible) or abstractions.
    - Manual check: Ensure tests pass.
    - Notes: Added SnapSwipeViewModel unit tests with a fake PhotoDataSource; uses Robolectric + coroutines-test.

[x] 7.4 – Final verification
    - Run through these manual flows:
      - First run → permission request → main screen.
      - Swiping left/right/up on multiple photos.
      - Changing sort order and verifying it.
      - Deleting photos and confirming deletion in the device gallery.
    - Note any issues and create new checklist items for fixes or improvements.
    - Notes: Automated checks are green; please run the above flows on device/emulator to confirm UX and gallery effects since device verification is not available in this environment.

------------------------------------------------------------
PHASE 8 – Localization Polish
------------------------------------------------------------

[x] 8.1 – Add app language picker and locale config
    - Add locale_config and manifest declaration for supported locales.
    - Provide Settings option to change the app language (system default, en/ko/fr/es/de/zh-CN) using AppCompatDelegate application locales.
    - Manual check: Switching language via Settings or Android's App language updates UI strings immediately.
    - Notes: Locale config added; Settings now contains an App language section that updates per-app locale using AppCompatDelegate.

[x] 8.2 – Persist app language and default to device locale
    - Enable AppCompat autoStoreLocales service so system-level app language settings are stored.
    - Set default/fallback locale explicitly (English) via resources.properties.
    - Manual check: Fresh install follows device language; system “App language” setting persists chosen locale.

------------------------------------------------------------
PHASE 9 – Interaction Modes
------------------------------------------------------------

[x] 9.1 – Add “Scroll and delete” mode
    - Add a settings toggle between “Swipe to choose” (existing) and “Scroll and delete”.
    - Scroll and delete: right-to-left deletes; left-to-right shares; vertical swipes move forward/back with vertical slide animation; undo/back button hidden.
    - Manual check: Switching modes updates gestures/UI immediately; share/delete still work; vertical swipes navigate without reloading.

------------------------------------------------------------
PHASE 10 – Settings UI Polish
------------------------------------------------------------

[x] 10.1 – Wireframe-inspired Settings refresh
    - Rebuild Settings screen layout to mirror the wireframe kit style with a profile header, grouped cards, chip toggles, and clearer links.
    - Manual check: New layout matches the wireframe aesthetic; sort order, interaction mode, and delete mode still update preferences correctly; support/about/what’s new dialogs remain accessible.

[x] 10.2 – Theme selector (light/dark/system)
    - Add a Settings control for appearance and persist it.
    - Wire the preference through SnapSwipeTheme so the entire app follows the chosen light/dark/system mode.

------------------------------------------------------------
PHASE 11 – Platform Compliance
------------------------------------------------------------

[x] 11.1 – Target API 36 (Android 16) for Google Play compliance
    - Raise compileSdk and targetSdk from 35 to 36; bump to 1.2.1 / 010201.
    - Android 16 removes the edge-to-edge opt-out and ignores android:statusBarColor, so drop the
      deprecated theme attribute and add navigationBarsPadding() to the main screen's bottom
      overlay (photo counter, delete/keep/undo buttons) so it clears the gesture bar / nav bar.
    - Manual check: On an Android 16 device, the header clears the status bar and the bottom
      controls clear the navigation bar in both gesture and 3-button modes; photo load, delete,
      and share still work.

[x] 11.2 – Set a release version newer than production
    - Prepare 1.3.1 / 010301 targeting API 36 to supersede Play production 1.3.0 / 010300.

[x] 11.3 – Finish the pre-release audit fixes
    - Android 11 and later use Trash and verify restored photos through MediaStore.
    - Keep a canceled batch available for review, retry, or discard, and re-query MediaStore on restart.
    - Handle selected-photo access and older Android delete consent. Label review controls for accessibility and translate the missing strings.
    - Exercise approval, denial, failure, restore, queue recovery, and permanent delete behavior in regression tests.

------------------------------------------------------------
PHASE 12 – Review Navigation
------------------------------------------------------------

[x] 12.1 – Resume a review and choose a timeline starting point
    - Save the current photo, review range, kept choices, and unsubmitted delete queue on the device.
    - Match the saved state against the currently accessible MediaStore library and offer Resume or Start from beginning when reopening the app with an unfinished review.
    - Browse the chronological thumbnail grid and scrub to a date. Start a new review with the selected photo.

[x] 12.2 – Keep the resume prompt out of Android system flows
    - Offer Resume after leaving the app or reopening a saved review.
    - Return directly to the review after Android photo permission, Trash, and restore dialogs.
    - Verified the warm and cold resume choices, queue cancellation, and Trash and restore on the attached Android 13 Galaxy using disposable fixtures.

[x] 12.3 – Preserve review progress during gallery retries and refreshes
    - Restore saved progress when the first gallery query succeeds on retry.
    - Keep review decisions made while a background gallery refresh is running.
    - Cover both paths with regression tests.

## Phase 13 · Material 3 UI update

Keep photo review at the center of the app. Give navigation, progress, and decisions consistent Material 3 surfaces. Preserve both interaction modes, saved progress, the delete queue, and Android's permission, Trash, and Sharesheet flows.

### 13.0 · Design audit (complete)

Snap Swipe already uses Material 3 for its theme, settings app bar, buttons, dialogs, share sheet, and snackbars. The Compose BOM `2023.10.01` resolves Material 3 to `1.1.2`. The audit reviewed the 1.4.0 source and inspected permissions, settings, the empty gallery, photo review, timeline, share sheet, and queue on an Android 16 emulator with synthetic photos.

| Priority | Area | Finding |
| --- | --- | --- |
| P1 | [Photo review](app/src/main/java/com/snapswipe/app/ui/MainSwipeScreen.kt#L294) | White progress and action icons sit on translucent overlays whose contrast depends on the photo. On a white test image, the progress label's local background gave white text 3.45:1 contrast, and the white status icons disappeared. The header is fixed at 42 dp and its icon buttons at 44 dp. The bottom controls use calculated offsets and the photo uses `ContentScale.Crop`, which can hide parts of the photo being reviewed. |
| P1 | [Review controls](app/src/main/java/com/snapswipe/app/ui/MainSwipeScreen.kt#L241) | Share in both modes and Previous in Scroll and delete have gesture paths only. The close icon and generic Delete label also obscure the difference between queueing a photo, moving it to Trash, and permanent deletion. |
| P1 | [Queue and timeline](app/src/main/java/com/snapswipe/app/ui/MainSwipeScreen.kt#L465) | The queue is a capped list inside a confirmation dialog with per-item Keep, clear, and commit actions. The [timeline](app/src/main/java/com/snapswipe/app/ui/PhotoTimelineDialog.kt#L59) also lives inside an alert dialog and gives the scrubber a general label without announcing the preview date as its value. |
| P2 | [Theme and system bars](app/src/main/java/com/snapswipe/app/ui/theme/Theme.kt#L14) | Dynamic color and appearance preferences are in place. The fallback overrides only three accent roles while retaining default matching roles. The review screen opts out of Scaffold content insets and handles status and navigation padding in separate overlays. |
| P2 | [Settings](app/src/main/java/com/snapswipe/app/ui/SettingsScreen.kt#L87) | Single-choice preferences are styled as independent filter chips in fixed rows. At 200% font scale on the emulator, the Dark chip broke across three lines. The app bar uses a trailing Done action for settings that save immediately, and About still lists target API 35 while the project targets 36. |
| P2 | [Permissions and review states](app/src/main/java/com/snapswipe/app/ui/SnapSwipeApp.kt#L171) | The first-run screen gives both actions filled-button prominence, and Continue without access opens Settings. On the empty-library test, the app displayed What's New and the gesture tutorial before the empty state. The empty and completed states give competing actions the same weight. Selected-photo access appears as a separate button over the image. |

The touch-target and contrast findings need verification across devices and themes. Set a project target of 4.5:1 for text and 3:1 for meaningful non-text elements, with interactive targets at least 48 dp. [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [Android design accessibility](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility)

### Review controls and responsive layouts

- [x] **13.0a · Improve visibility in the existing review layout.** Align Delete and Keep or Next in one row with opaque coral and green fills. Build the taller, translucent review bar with top padding and solid icon buttons. Put Undo in the leading slot and move the former Home action to Start from beginning in the review menu. Keep the current phone portrait photo framing and fit photos to wider or shorter viewports. Bound wide-screen settings and controls, wrap long settings choices, and keep permission content and review dialogs usable in short windows.
  - Verified phone portrait and landscape, an 800 dp tablet viewport, and 200% font scale at 320 dp wide on an Android 16 emulator. Checked both review modes, Undo, the restart menu, timeline rotation, the review queue, Settings, and first-run permissions. The debug build and all 21 unit tests passed. Lint reported zero errors.
- [x] **13.0b · Neutral menus and timeline date navigation.** Use a fixed gray Light palette and charcoal Dark palette for app surfaces. Keep the Use device option for picking between them. Sort the timeline from oldest to newest regardless of review order. Offer year, month, and day choices drawn from accessible photos. Jump to the first photo in the selected period and highlight it in the grid.

### Follow-up ideas from the audit

- [ ] **13.1 · Theme and layout foundation.** Use the neutral light and dark color roles and the Material 3 type and shape scales for shared components. Establish an edge-to-edge inset strategy with safe content around system bars and cutouts. Verify system bar icon contrast with forced Light and Dark. Start with the installed Material 3 components. Upgrade Compose, Kotlin, and the matching compiler in a separate change only if a chosen component requires it. [Material 3 theming](https://developer.android.com/develop/ui/compose/designsystems/material3), [Material 3 insets](https://developer.android.com/develop/ui/compose/system/material-insets), [Compose edge-to-edge setup](https://developer.android.com/develop/ui/compose/system/setup-e2e)
- [ ] **13.2 · Photo review and action hierarchy.** Use a small Material 3 top app bar and a stable bottom action surface with progress, a queue entry when needed, and labeled Keep, Add to queue, Move to Trash, or Delete actions as appropriate. Make Share available by tap in both modes, and Previous and Next available by tap in Scroll and delete. Keep gesture mappings and Undo, use density-aware swipe thresholds, and review system back gesture interaction. Fit the full image into the review area so every edge can be inspected. Give loading, empty, failure, and completion states one clear primary action per state. Keep content scrollable or wrap controls on compact windows. Give the current reset action a text label and place it with review options instead of presenting it as a Home destination. [Compose app bars](https://developer.android.com/develop/ui/compose/components/app-bars), [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [ ] **13.3 · Queue, timeline, and review dialogs.** Move the pending queue and timeline to dedicated review destinations with Material app bars and scrollable content. Use a persistent queue footer with the count and platform-appropriate commit action. Give each queue row a distinct accessible label for removing that photo from the queue. Highlight the current photo and expose the timeline's date or position through slider semantics. Keep `AlertDialog` for short decisions such as resume, restarting with a queued batch, and permanent deletion. Use explicit copy for any action that clears the queue. Keep the swipe-up share sheet in Swipe to choose and route sharing through Android's Sharesheet. [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog), [Compose bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
- [ ] **13.4 · Settings and photo access.** Group preferences into Material 3 list rows for Appearance, Review, Photo access, and About and support. Use single-selection controls with a radio-list fallback when translated labels or font scaling make horizontal segments cramped. Keep the queued-delete switch with a tappable labeled row and use leading Back in the Settings app bar. Update the About API value from the build configuration. Make Grant photo access the primary permission action, label the secondary action Open Snap Swipe settings, and show selected-photo access in a persistent inline notice with Manage access. Show gesture guidance when the first photo can be reviewed. [Segmented buttons](https://developer.android.com/develop/ui/compose/components/segmented-button), [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- [ ] **13.5 · Verify before shipping.** Capture before/after review, settings, queue, timeline, and permission states. Check a compact portrait phone, compact-height landscape, and a wider window. Exercise gesture and three-button navigation, light/dark/dynamic colors, pre-Android 12 fallback, 200% font scale, a long translated locale, and forced RTL layout. Confirm readable controls over white, black, and busy photos, 48 dp targets with no overlap, and TalkBack focus, labels, state, and operation without swiping. Run `./gradlew testDebugUnitTest lintDebug assembleDebug` and add focused Compose coverage for tap alternatives, action labels, and queue/timeline navigation. On test photos, verify permission denial and partial selection, both review modes, warm and cold resume, queue cancellation and retry, Trash and restore, older Android deletion, and share. [Adaptive window size classes](https://developer.android.com/develop/adaptive-apps/guides/use-window-size-classes), [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)

## Phase 14 · Google Play listing

- [x] **14.1 · Refresh the English listing.** Write short and full descriptions for the free, ad-free photo cleaner. Capture and verify four phone screenshots for production 1.3.0 and five for release 1.4.0. The newer set includes the timeline and saved review. Save the production copy and screenshots in Play Console. [Listing assets and upload order](marketing/google-play/README.md)
- [x] **14.2 · Submit the English listing update.** Play Console lists the English copy, phone screenshots, and app-level trader-status declaration under Changes in review.

- [x] **14.3 · Expand bundled localization.** Add Indonesian, Turkish, Bengali, and Arabic resources and Android app-language entries. Keep the Delete and Keep controls aligned with their physical swipe directions in Arabic. Prepare localized Play listing copy for the four languages and build 1.4.2 / 14002 for signing.
  - Verified all 106 resource keys and format arguments in each locale. The emulator displayed the new Settings and review labels in all four languages. The Arabic timeline opened, a right swipe kept the photo, and a left swipe opened Android’s Trash confirmation.
- [x] **14.4 · Submit the localized listings.** Play Console lists Indonesian, Turkish, Bengali, and Arabic under Changes in review alongside the English listing and trader-status declaration.
- [x] **14.5 · Package and verify offline app languages.** Disable language splits and Gemini app-string translation in Play Console. Verify all 18 locale selections offline on an emulator using APKs generated from the release bundle.
- [ ] **14.6 · Verify Google Play delivery.** Sign the release bundle with the upload key and verify it on a Play testing track.
