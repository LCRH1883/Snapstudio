# Snap Swipe – Build Plan

This file tracks the implementation of the Snap Swipe Android app.

For each step:
- Codex should implement the described changes.
- After implementation, run a build and (where applicable) run the app to manually test.
- When a step is done, mark its checkbox as `[x]` and add any notes.
- Important: keep this file in sync with AGENTS.md and claude.md.

------------------------------------------------------------
PHASE 0 – Documentation & Project Setup
------------------------------------------------------------

[x] 0.1 – Create agent docs
    - Create AGENTS.md with the contents from the initial instructions.
    - Create claude.md with the contents from the initial instructions.
    - Ensure both files are committed/available in the repository.
    - Manual check: AGENTS.md and claude.md exist at repo root and render correctly as Markdown.

[ ] 0.2 – Create Android Studio project for Snap Swipe
    - Project name: Snap Swipe.
    - Package name: e.g. com.example.snapswipe (or equivalent).
    - Language: Kotlin.
    - Min SDK: 26.
    - Target/compile SDK: latest stable available.
    - Template: a Compose-based template (Empty Compose Activity, or Empty Activity with Jetpack Compose enabled).
    - Manual check: Project builds successfully; main activity runs and shows the default Compose preview content.

[ ] 0.3 – Configure Jetpack Compose dependencies
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

[ ] 0.4 – Create initial app scaffold
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

[ ] 1.1 – Implement permissions screen UI
    - Create a composable PermissionsScreen:
      - Show app name and a short explanation: Snap Swipe needs access to photos to help you clean them up.
      - Show a primary button: “Grant photo access”.
      - Optional secondary button: “Continue without access” that shows an informative message or exits.
    - Manual check: PermissionsScreen displays correctly when the app starts.

[ ] 1.2 – Implement runtime permission request logic
    - Use rememberLauncherForActivityResult for requesting:
      - READ_MEDIA_IMAGES on Android 13+.
      - READ_EXTERNAL_STORAGE on older versions (if needed).
    - When the user taps “Grant photo access”:
      - Trigger permission request.
    - On result:
      - If granted: navigate to "main" screen.
      - If denied: show appropriate error message and allow retry.
    - Manual check: On an emulator/device, verify permission dialog appears and navigation behavior is correct.

[ ] 1.3 – Permission gate before main screen
    - Ensure navigation logic checks permissions before showing the main swipe UI.
    - If the permission is not granted at app start:
      - Start on "permissions" route.
    - If permission is granted:
      - Start on "main" route.
    - Manual check: Reinstall app, test both flows (grant vs deny).

------------------------------------------------------------
PHASE 2 – Photo Data Layer (MediaStore)
------------------------------------------------------------

[ ] 2.1 – Define photo model
    - Create a data class PhotoItem with at least:
      - id: Long (MediaStore ID).
      - uri: Uri.
      - dateTaken: Long? (or similar).
    - Manual check: Data class compiles and is used only within the data layer initially.

[ ] 2.2 – Implement MediaStore photo repository
    - Create a repository, e.g. PhotoRepository, responsible for:
      - Querying MediaStore for images from EXTERNAL_CONTENT_URI.
      - Mapping cursor results to PhotoItem objects.
      - Respecting a sort order: NEWEST_FIRST or OLDEST_FIRST.
    - Define an enum SortOrder { NEWEST_FIRST, OLDEST_FIRST }.
    - Implement a method:
      - suspend fun loadPhotos(sortOrder: SortOrder): List<PhotoItem>
    - Handle edge cases (no photos, null dates).
    - Manual check: Temporarily log or display the count of loaded photos to verify it works.

[ ] 2.3 – Wire repository into a ViewModel
    - Create a SnapSwipeViewModel (or MainViewModel) that:
      - Holds the list of PhotoItem loaded from the repository.
      - Exposes the current index and current photo.
      - Provides functions: nextPhoto(), keepCurrent(), trashCurrent() (initially only changing index; deletion to be implemented later).
    - Manual check: In a debug placeholder main screen, show:
      - Current photo index and total count.
      - Buttons for “Keep” and “Delete” that just advance to next photo for now.

------------------------------------------------------------
PHASE 3 – Settings (Sort Order) with DataStore
------------------------------------------------------------

[ ] 3.1 – Set up DataStore Preferences
    - Add DataStore initialization in a suitable place (e.g. a singleton or repository).
    - Define a preference key for photo sort order (string or int).
    - Provide functions to:
      - Read current sort order as a Flow<SortOrder>.
      - Persist updated sort order.
    - Manual check: Create a temporary UI or log to confirm the sort order can be read and updated.

[ ] 3.2 – Implement Settings screen UI
    - Create SettingsScreen composable with:
      - A simple top app bar labeled “Settings”.
      - A section for “Photo review order” with:
        - Radio buttons or a toggle for:
          - “Newest to oldest” (default).
          - “Oldest to newest”.
      - A brief “About Snap Swipe” text (1–2 sentences) and a placeholder version string.
    - Manual check: SettingsScreen renders correctly and reads the current sort order from DataStore.

[ ] 3.3 – Connect Settings to navigation and data
    - Add navigation from the main screen to Settings (e.g., top app bar icon).
    - When the sort order changes:
      - Persist it via DataStore.
      - Trigger reload of photos in the ViewModel with the new order.
    - Manual check: Changing the sort order in Settings and returning to the main screen should show photos in the correct order (verify by comparing to the device gallery).

------------------------------------------------------------
PHASE 4 – Main Swipe UI with Gestures
------------------------------------------------------------

[ ] 4.1 – Basic main screen layout
    - Create MainSwipeScreen composable that:
      - Displays the current photo using Coil’s AsyncImage (or equivalent).
      - Shows optional overlay UI:
        - A simple top bar with app title and Settings icon.
        - A small hint text for gestures (e.g., “Swipe left to delete, right to keep, up to share”).
    - Manual check: With fake or debug images, confirm the layout works in both portrait and (optionally) landscape.

[ ] 4.2 – Implement swipe gesture handling
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

[ ] 4.3 – “No photos” and “end of list” states
    - When there are no photos at all:
      - Show a friendly empty state: “No photos found. Take some photos and come back!”.
    - When the user reaches the end of the list:
      - Show a message like: “You’ve reviewed all photos in this run.”
      - Allow restarting or going back to settings.
    - Manual check: Use a test device/emulator with few or no photos to verify these paths.

------------------------------------------------------------
PHASE 5 – Delete / Trash and Keep Behavior
------------------------------------------------------------

[ ] 5.1 – Implement delete/trash operation
    - In the repository, implement a function to delete a given PhotoItem:
      - For newer Android versions, use the recommended MediaStore APIs to request deletion, which may show a system confirmation dialog.
      - For older versions, use ContentResolver.delete() on the photo URI.
    - In the ViewModel, wire trashCurrent():
      - Call the repository delete method for the current photo.
      - On success, move to the next photo.
      - Handle errors (e.g., log or show a simple message).
    - Manual check: On a test device (with non-critical photos), swipe to delete and verify the photo is actually removed from the system gallery.

[ ] 5.2 – Implement keep behavior
    - For keepCurrent(), do not modify the photo at all.
    - Simply advance to the next photo.
    - Manual check: After swiping to keep, verify the photo still appears in the system gallery and the app shows the next one.

[ ] 5.3 – Avoid re-showing processed photos in the same run
    - Optional but recommended:
      - Maintain an in-memory set or simple state that tracks which photos were processed during the current session.
      - Do not show processed photos again until the app is restarted or the list is reloaded intentionally.
    - Manual check: Repeated swiping in the same run should not loop the same photo.

------------------------------------------------------------
PHASE 6 – Share Bottom Menu
------------------------------------------------------------

[ ] 6.1 – Implement bottom sheet UI
    - When the user swipes up:
      - Open a ModalBottomSheet (or similar).
      - Show actions:
        - “Share photo” – triggers system share.
        - “Cancel” – closes the sheet.
    - Manual check: Swiping up on a photo shows and hides the bottom sheet correctly.

[ ] 6.2 – Implement system share intent
    - Implement a helper function to share the current photo:
      - Use Intent.ACTION_SEND with the photo’s content URI.
      - Use Intent.createChooser so the user can pick the app (Google Photos, Messages, etc.).
      - Ensure you use a content URI that other apps can access (MediaStore URI should work; use appropriate flags).
    - Manual check: Share a photo and confirm that target apps receive and display it.

------------------------------------------------------------
PHASE 7 – Polish and Testing
------------------------------------------------------------

[ ] 7.1 – Visual polish
    - Improve spacing, typography, and colors using Material3.
    - Ensure dark mode looks reasonable.
    - Add a simple app icon placeholder for Snap Swipe.
    - Manual check: Run on multiple screen sizes to ensure UI looks acceptable.

[ ] 7.2 – Basic error handling and logging
    - Add safe error handling for:
      - MediaStore query failures.
      - Permission issues.
      - Delete failures.
    - Use Android logging (e.g., Log.d/e) where helpful.
    - Manual check: Manually trigger error paths where reasonable (e.g., revoke permission) and confirm app doesn’t crash.

[ ] 7.3 – Simple tests
    - Add unit tests for:
      - ViewModel logic (advancing index, handling keep/delete, reacting to sort order changes).
      - Simple repository tests (where possible) or abstractions.
    - Manual check: Ensure tests pass.

[ ] 7.4 – Final verification
    - Run through these manual flows:
      - First run → permission request → main screen.
      - Swiping left/right/up on multiple photos.
      - Changing sort order and verifying it.
      - Deleting photos and confirming deletion in the device gallery.
    - Note any issues and create new checklist items for fixes or improvements.
