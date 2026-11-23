package com.snapswipe.app.ui

import android.Manifest
import android.os.Build
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snapswipe.app.BuildConfig
import com.snapswipe.app.data.SortOrder
import com.snapswipe.app.data.SortOrderPreferences
import com.snapswipe.app.data.PhotoItem
import com.snapswipe.app.data.DeleteMode
import com.snapswipe.app.data.InteractionMode
import com.snapswipe.app.data.ThemeMode
import com.snapswipe.app.R
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.unit.dp
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import com.snapswipe.app.ui.theme.SnapSwipeTheme

private const val ROUTE_PERMISSIONS = "permissions"
private const val ROUTE_MAIN = "main"
private const val ROUTE_SETTINGS = "settings"

private enum class PhotoAccess { NONE, SELECTED, FULL }

@Composable
fun SnapSwipeApp(
    returnRevision: Long = 0L,
    onSystemFlowChanged: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val sortOrderPreferences = remember { SortOrderPreferences(context) }
    val themeMode by sortOrderPreferences.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    var photoAccess by remember { mutableStateOf(readPhotoAccess(context)) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var accessRevision by remember { mutableStateOf(0) }
    val grantLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        onSystemFlowChanged(false)
        photoAccess = readPhotoAccess(context)
        permissionDenied = photoAccess == PhotoAccess.NONE
        accessRevision++
    }

    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val previous = photoAccess
                photoAccess = readPhotoAccess(context)
                if (photoAccess != PhotoAccess.NONE || previous != photoAccess) accessRevision++
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(photoAccess, route) {
        if (photoAccess != PhotoAccess.NONE && route == ROUTE_PERMISSIONS) {
            permissionDenied = false
            navController.navigate(ROUTE_MAIN) {
                popUpTo(ROUTE_PERMISSIONS) { inclusive = true }
                launchSingleTop = true
            }
        } else if (photoAccess == PhotoAccess.NONE &&
            (route == ROUTE_MAIN ||
                navController.previousBackStackEntry?.destination?.route == ROUTE_MAIN)
        ) {
            navController.navigate(ROUTE_PERMISSIONS) {
                popUpTo(ROUTE_MAIN) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val requestAccess = {
        onSystemFlowChanged(true)
        grantLauncher.launch(photoPermissions())
    }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        onSystemFlowChanged(false)
    }
    val openAppSettings = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        onSystemFlowChanged(true)
        settingsLauncher.launch(intent)
    }
    val manageAccess = if (photoAccess == PhotoAccess.SELECTED) requestAccess else openAppSettings

    SnapSwipeTheme(themeMode = themeMode) {
        NavHost(navController = navController, startDestination = ROUTE_PERMISSIONS) {
            composable(ROUTE_PERMISSIONS) {
                PermissionsScreen(
                    onGrantAccess = requestAccess,
                    onSkip = { navController.navigate(ROUTE_SETTINGS) },
                    onOpenAppSettings = openAppSettings,
                    permissionDenied = permissionDenied
                )
            }
            composable(ROUTE_MAIN) {
                if (photoAccess != PhotoAccess.NONE) {
                    val viewModel: SnapSwipeViewModel = viewModel(
                        factory = remember { SnapSwipeViewModelFactory(context.applicationContext) }
                    )
                    MainScreen(
                        onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                        onRequestPermissions = manageAccess,
                        hasLimitedAccess = photoAccess == PhotoAccess.SELECTED,
                        accessRevision = accessRevision,
                        returnRevision = returnRevision,
                        onSystemFlowChanged = onSystemFlowChanged,
                        viewModel = viewModel
                    )
                }
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    onDone = { navController.popBackStack() },
                    onManagePhotoAccess = manageAccess
                )
            }
        }
    }
}

@Composable
private fun PermissionsScreen(
    onGrantAccess: () -> Unit,
    onSkip: () -> Unit,
    onOpenAppSettings: () -> Unit,
    permissionDenied: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.permission_rationale),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (permissionDenied) {
            Text(
                text = stringResource(R.string.permission_denied_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = onGrantAccess) { Text(stringResource(R.string.grant_photo_access)) }
        if (permissionDenied) {
            TextButton(onClick = onOpenAppSettings) { Text(stringResource(R.string.manage_photo_access)) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSkip) {
            Text(stringResource(R.string.continue_without_access))
        }
    }
}

@Composable
private fun MainScreen(
    onOpenSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    hasLimitedAccess: Boolean,
    accessRevision: Int,
    returnRevision: Long,
    onSystemFlowChanged: (Boolean) -> Unit,
    viewModel: SnapSwipeViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val sortOrderPreferences = remember { SortOrderPreferences(context) }
    val sortOrder by sortOrderPreferences.sortOrderFlow.collectAsState(initial = null as SortOrder?)
    val deleteModePref by sortOrderPreferences.deleteModeFlow.collectAsState(initial = uiState.deleteMode)
    val instructionsSeen by sortOrderPreferences.instructionsSeenFlow.collectAsState(initial = null as Boolean?)
    val interactionMode by sortOrderPreferences.interactionModeFlow.collectAsState(initial = InteractionMode.SWIPE_TO_CHOOSE)
    val lastSeenVersion by remember {
        sortOrderPreferences.lastSeenVersionFlow.map { it.orEmpty() }
    }.collectAsState(initial = null as String?)
    val coroutineScope = rememberCoroutineScope()
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        onSystemFlowChanged(false)
        val success = result.resultCode == android.app.Activity.RESULT_OK
        viewModel.onDeleteCompleted(success)
    }
    var pendingPermanentDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var waitingForWriteAccess by rememberSaveable { mutableStateOf(false) }
    val performDelete: (Boolean) -> Unit = { batch ->
        if (batch) viewModel.commitQueuedDeletes() else viewModel.trashCurrent()
    }
    val writeAccessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onSystemFlowChanged(false)
        val request = pendingPermanentDelete
        pendingPermanentDelete = null
        waitingForWriteAccess = false
        if (granted && request != null) performDelete(request == "batch")
    }
    val requestDelete: (Boolean) -> Unit = { batch ->
        if (viewModel.supportsTrash || (!batch && uiState.deleteMode == DeleteMode.QUEUED)) {
            performDelete(batch)
        } else {
            pendingPermanentDelete = if (batch) "batch" else "single"
        }
    }
    pendingPermanentDelete?.takeUnless { waitingForWriteAccess || uiState.offerResume }?.let { request ->
        AlertDialog(
            onDismissRequest = { pendingPermanentDelete = null },
            title = { Text(stringResource(R.string.permanent_delete_title)) },
            text = { Text(stringResource(R.string.permanent_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        !hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        waitingForWriteAccess = true
                        onSystemFlowChanged(true)
                        writeAccessLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        pendingPermanentDelete = null
                        performDelete(request == "batch")
                    }
                }) { Text(stringResource(R.string.delete_label)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingPermanentDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    if (uiState.offerResume) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.resume_review_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.resume_review_description))
                    if (uiState.queuedDeletes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.discard_queue))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::resumeReview) {
                    Text(stringResource(R.string.resume_review))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::startFromBeginning) {
                    Text(stringResource(R.string.start_from_beginning))
                }
            }
        )
    }
    var pendingTimelineStart by remember { mutableStateOf<PhotoItem?>(null) }
    pendingTimelineStart?.takeUnless { uiState.offerResume }?.let { photo ->
        AlertDialog(
            onDismissRequest = { pendingTimelineStart = null },
            title = { Text(stringResource(R.string.start_here)) },
            text = { Text(stringResource(R.string.discard_queue)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingTimelineStart = null
                    viewModel.startAt(photo)
                }) { Text(stringResource(R.string.start_here)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingTimelineStart = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    var confirmRestart by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.offerResume) {
        if (uiState.offerResume) {
            confirmRestart = false
            pendingTimelineStart = null
            if (!waitingForWriteAccess) pendingPermanentDelete = null
        }
    }
    if (confirmRestart && !uiState.offerResume) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text(stringResource(R.string.restart)) },
            text = { Text(stringResource(R.string.discard_queue)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestart = false
                    viewModel.restart()
                }) { Text(stringResource(R.string.restart)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    var dismissedWhatsNew by rememberSaveable { mutableStateOf(false) }
    val showWhatsNew = lastSeenVersion?.let { it != BuildConfig.VERSION_NAME } == true && !dismissedWhatsNew

    LaunchedEffect(sortOrder) {
        sortOrder?.let(viewModel::ensurePhotos)
    }
    LaunchedEffect(deleteModePref) {
        viewModel.setDeleteMode(deleteModePref)
    }
    LaunchedEffect(uiState.pendingDeleteIntent) {
        viewModel.consumeApprovalRequest()?.let { intent ->
            val request = IntentSenderRequest.Builder(intent)
                .setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION, Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                .build()
            onSystemFlowChanged(true)
            deleteLauncher.launch(request)
        }
    }
    LaunchedEffect(returnRevision) {
        viewModel.offerResumeAfterReturn(returnRevision)
    }
    LaunchedEffect(accessRevision) {
        if (accessRevision > 0) viewModel.refreshPhotoAccess()
    }
    MainSwipeScreen(
        uiState = uiState,
        onOpenSettings = onOpenSettings,
        onRequestPermissions = onRequestPermissions,
        onKeep = { viewModel.keepCurrent() },
        onDelete = { requestDelete(false) },
        onUndo = { viewModel.undoLast() },
        onHome = { viewModel.goHome() },
        onShare = { sharePhoto(context, uiState.currentPhoto) },
        interactionMode = interactionMode,
        onRestart = {
            if (uiState.queuedDeletes.isEmpty()) viewModel.restart() else confirmRestart = true
        },
        onReload = { viewModel.reload() },
        queuedDeleteCount = uiState.queuedDeletes.size,
        onCommitQueuedDeletes = { requestDelete(true) },
        onRemoveFromQueue = viewModel::removeFromQueue,
        onDiscardQueue = viewModel::discardQueue,
        onStartAt = { photo ->
            if (uiState.queuedDeletes.isEmpty()) viewModel.startAt(photo) else pendingTimelineStart = photo
        },
        onClearOperationError = viewModel::clearOperationError,
        hasLimitedAccess = hasLimitedAccess,
        supportsTrash = viewModel.supportsTrash,
        showInstructions = !uiState.isLoading && !uiState.offerResume &&
            lastSeenVersion != null && instructionsSeen == false && !showWhatsNew,
        onDismissInstructions = {
            coroutineScope.launch { sortOrderPreferences.setInstructionsSeen(true) }
        },
        onScrollForward = { viewModel.stepForward() },
        onScrollBack = { viewModel.stepBack() },
        showWhatsNew = !uiState.isLoading && !uiState.offerResume && showWhatsNew,
        whatsNewVersionName = BuildConfig.VERSION_NAME,
        onDismissWhatsNew = {
            dismissedWhatsNew = true
            coroutineScope.launch {
                sortOrderPreferences.setLastSeenVersion(BuildConfig.VERSION_NAME)
            }
        }
    )
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED

private fun readPhotoAccess(context: Context): PhotoAccess = when {
    hasPermission(context, photoPermissions().first()) -> PhotoAccess.FULL
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        hasPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> PhotoAccess.SELECTED
    else -> PhotoAccess.NONE
}

private fun photoPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun sharePhoto(context: Context, photo: PhotoItem?) {
    if (photo == null) return
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, photo.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_photo))
        .addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    try {
        context.startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
        Log.w("SnapSwipeApp", "No activity to handle share intent", e)
    }
}
