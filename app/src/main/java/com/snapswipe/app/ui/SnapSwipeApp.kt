package com.snapswipe.app.ui

import android.Manifest
import android.os.Build
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
import androidx.compose.material3.Surface
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snapswipe.app.data.SortOrder
import com.snapswipe.app.data.SortOrderPreferences
import com.snapswipe.app.data.PhotoItem
import com.snapswipe.app.data.DeleteMode
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.unit.dp
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch

private const val ROUTE_PERMISSIONS = "permissions"
private const val ROUTE_MAIN = "main"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun SnapSwipeApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    var hasPhotoPermission by remember {
        mutableStateOf(isPhotoPermissionGranted(context))
    }
    var permissionDenied by remember { mutableStateOf(false) }

    LaunchedEffect(hasPhotoPermission) {
        val target = if (hasPhotoPermission) ROUTE_MAIN else ROUTE_PERMISSIONS
        if (hasPhotoPermission) {
            permissionDenied = false
        }
        navController.navigate(target) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        NavHost(
            navController = navController,
            startDestination = ROUTE_PERMISSIONS
        ) {
            permissionsRoute(
                onGrantAccess = {
                    hasPhotoPermission = true
                    permissionDenied = false
                },
                onSkip = {
                    // Inform the user; keeping placeholder until a dedicated UI is added.
                },
                onPermissionDenied = {
                    permissionDenied = true
                },
                hasPermission = hasPhotoPermission,
                permissionDenied = permissionDenied
            )
            mainRoute(
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                onBackToPermissions = {
                    hasPhotoPermission = false
                }
            )
            settingsRoute(
                onDone = { navController.popBackStack() }
            )
        }
    }
}

private fun NavGraphBuilder.permissionsRoute(
    onGrantAccess: () -> Unit,
    onSkip: () -> Unit,
    onPermissionDenied: () -> Unit,
    hasPermission: Boolean,
    permissionDenied: Boolean
) {
    composable(ROUTE_PERMISSIONS) {
        val permissionToRequest = remember { photoPermissionForDevice() }
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                onGrantAccess()
            } else {
                onPermissionDenied()
            }
        }

        PermissionsScreen(
            hasPermission = hasPermission,
            onGrantAccess = {
                launcher.launch(permissionToRequest)
            },
            onSkip = onSkip,
            permissionDenied = permissionDenied
        )
    }
}

private fun NavGraphBuilder.mainRoute(
    onOpenSettings: () -> Unit,
    onBackToPermissions: () -> Unit
) {
    composable(ROUTE_MAIN) {
        val context = LocalContext.current
        val viewModel: SnapSwipeViewModel = viewModel(
            factory = remember { SnapSwipeViewModelFactory(context.contentResolver) }
        )
        MainScreen(
            onOpenSettings = onOpenSettings,
            onRequestPermissions = onBackToPermissions,
        viewModel = viewModel
    )
}
}

private fun NavGraphBuilder.settingsRoute(
    onDone: () -> Unit
) {
    composable(ROUTE_SETTINGS) {
        SettingsScreen(onDone = onDone)
    }
}

@Composable
private fun PermissionsScreen(
    hasPermission: Boolean,
    onGrantAccess: () -> Unit,
    onSkip: () -> Unit,
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
            text = "Snap Swipe needs photo access to help you clean up your gallery.",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (permissionDenied && !hasPermission) {
            Text(
                text = "Permission is required to load your photos. Please grant access.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = onGrantAccess, enabled = !hasPermission) {
            Text(if (hasPermission) "Permission granted" else "Grant photo access")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSkip) {
            Text("Continue without access")
        }
    }
}

@Composable
private fun MainScreen(
    onOpenSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    viewModel: SnapSwipeViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val sortOrderPreferences = remember { SortOrderPreferences(context) }
    val sortOrder by sortOrderPreferences.sortOrderFlow.collectAsState(initial = SortOrder.NEWEST_FIRST)
    val deleteModePref by sortOrderPreferences.deleteModeFlow.collectAsState(initial = DeleteMode.IMMEDIATE)
    val instructionsSeen by sortOrderPreferences.instructionsSeenFlow.collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val success = result.resultCode == android.app.Activity.RESULT_OK
        viewModel.onDeleteCompleted(success)
    }

    LaunchedEffect(sortOrder) {
        viewModel.ensurePhotos(sortOrder)
    }
    LaunchedEffect(deleteModePref) {
        viewModel.setDeleteMode(deleteModePref)
    }
    LaunchedEffect(uiState.pendingDeleteIntent) {
        uiState.pendingDeleteIntent?.let { intent ->
            deleteLauncher.launch(IntentSenderRequest.Builder(intent).build())
        }
    }
    MainSwipeScreen(
        uiState = uiState,
        onOpenSettings = onOpenSettings,
        onRequestPermissions = onRequestPermissions,
        onKeep = { viewModel.keepCurrent() },
        onDelete = { viewModel.trashCurrent() },
        onUndo = { viewModel.undoLast() },
        onHome = { viewModel.goHome() },
        onShare = { sharePhoto(context, uiState.currentPhoto) },
        onRestart = { viewModel.restart() },
        onReload = { viewModel.reload() },
        queuedDeleteCount = uiState.queuedDeletes.size,
        onCommitQueuedDeletes = { viewModel.commitQueuedDeletes() },
        showInstructions = !instructionsSeen,
        onDismissInstructions = {
            coroutineScope.launch { sortOrderPreferences.setInstructionsSeen(true) }
        }
    )
}

private fun isPhotoPermissionGranted(context: android.content.Context): Boolean {
    val permission = photoPermissionForDevice()
    return ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun photoPermissionForDevice(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun sharePhoto(context: Context, photo: PhotoItem?) {
    if (photo == null) return
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, photo.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(shareIntent, "Share photo")
    try {
        context.startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
        Log.w("SnapSwipeApp", "No activity to handle share intent", e)
    }
}
