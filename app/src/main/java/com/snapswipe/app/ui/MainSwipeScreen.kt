package com.snapswipe.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.snapswipe.app.R
import com.snapswipe.app.data.InteractionMode
import com.snapswipe.app.data.PhotoItem
import kotlin.math.abs

@Suppress(
    "UNUSED_VARIABLE",
    "UNUSED_VALUE",
    "ComposableLambdaParameterNaming",
    "ComposableInvocation"
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSwipeScreen(
    uiState: SnapSwipeUiState,
    onOpenSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
    onUndo: () -> Unit = {},
    onHome: () -> Unit = {},
    onShare: () -> Unit = {},
    interactionMode: InteractionMode = InteractionMode.SWIPE_TO_CHOOSE,
    onRestart: () -> Unit = {},
    onReload: () -> Unit = {},
    queuedDeleteCount: Int = 0,
    onCommitQueuedDeletes: () -> Unit = {},
    onRemoveFromQueue: (PhotoItem) -> Unit = {},
    onDiscardQueue: () -> Unit = {},
    onStartAt: (PhotoItem) -> Unit = {},
    onClearOperationError: () -> Unit = {},
    hasLimitedAccess: Boolean = false,
    supportsTrash: Boolean = true,
    showInstructions: Boolean = false,
    onDismissInstructions: () -> Unit = {},
    onScrollForward: () -> Unit = {},
    onScrollBack: () -> Unit = {},
    showWhatsNew: Boolean = false,
    whatsNewVersionName: String = "",
    onDismissWhatsNew: () -> Unit = {}
) {
    val isScrollMode = interactionMode == InteractionMode.SCROLL_AND_DELETE
    val hasPhotos = uiState.photos.isNotEmpty()
    val processedAll = !hasPhotos && uiState.totalCount > 0 && !uiState.isLoading
    var showQueue by remember { mutableStateOf(false) }
    var showTimeline by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(uiState.offerResume) {
        if (uiState.offerResume) {
            showTimeline = false
            showShareSheet = false
            showQueue = false
        }
    }
    LaunchedEffect(uiState.operationError) {
        uiState.operationError?.let { message ->
            snackbar.showSnackbar(context.getString(message), withDismissAction = true)
            onClearOperationError()
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var lastScrollDirection by remember { mutableStateOf(0) }
    var previousIndex by remember { mutableStateOf(uiState.currentIndex) }
    LaunchedEffect(isScrollMode) {
        if (isScrollMode) {
            showShareSheet = false
        }
    }
    LaunchedEffect(uiState.currentIndex) {
        val diff = uiState.currentIndex - previousIndex
        if (diff > 0) lastScrollDirection = 1
        else if (diff < 0) lastScrollDirection = -1
        previousIndex = uiState.currentIndex
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
        topBar = {},
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.loading_photos))
                    }
                }

                uiState.loadError != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(uiState.loadError),
                            color = MaterialTheme.colorScheme.error
                        )
                        val permissionError = uiState.loadError == R.string.permission_denied_message
                        Button(onClick = if (permissionError) onRequestPermissions else onReload) {
                            Text(stringResource(if (permissionError) R.string.check_permissions else R.string.refresh))
                        }
                    }
                }

                processedAll -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reviewed_run_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.reviewed_run_subtitle),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (queuedDeleteCount > 0) {
                            Button(onClick = { showQueue = true }, enabled = !uiState.mediaBusy) {
                                Text(stringResource(R.string.review_queue, queuedDeleteCount))
                            }
                        }
                        if (uiState.lastAction != null) {
                            TextButton(onClick = onUndo, enabled = !uiState.mediaBusy) {
                                Text(stringResource(R.string.undo))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onRestart, enabled = !uiState.mediaBusy) {
                                Text(stringResource(R.string.restart))
                            }
                            Button(onClick = onOpenSettings, enabled = !uiState.mediaBusy) {
                                Text(stringResource(R.string.settings))
                            }
                        }
                    }
                }

                !hasPhotos -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.no_photos_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.no_photos_subtitle),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onReload) { Text(stringResource(R.string.refresh)) }
                            Button(onClick = onOpenSettings) { Text(stringResource(R.string.settings)) }
                        }
                    }
                }

                hasPhotos -> {
                    val photo = uiState.currentPhoto
                    val horizontalThreshold = 60f
                    val verticalThreshold = 80f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(uiState.currentPhoto?.uri, interactionMode, uiState.mediaBusy, uiState.deleteMode) {
                                if (uiState.mediaBusy) return@pointerInput
                                var totalDx = 0f
                                var totalDy = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        totalDx = 0f
                                        totalDy = 0f
                                    },
                                    onDragEnd = {
                                        if (abs(totalDx) > abs(totalDy)) {
                                            when {
                                                totalDx > horizontalThreshold -> {
                                                    if (isScrollMode) {
                                                        onShare()
                                                    } else {
                                                        onKeep()
                                                    }
                                                }
                                                totalDx < -horizontalThreshold -> onDelete()
                                            }
                                        } else {
                                            when {
                                                totalDy < -verticalThreshold -> {
                                                    if (isScrollMode) {
                                                        lastScrollDirection = 1
                                                        onScrollForward()
                                                    } else {
                                                        showShareSheet = true
                                                    }
                                                }
                                                totalDy > verticalThreshold && isScrollMode -> {
                                                    lastScrollDirection = -1
                                                    onScrollBack()
                                                }
                                            }
                                        }
                                    }
                                ) { change, dragAmount ->
                                    val (dx, dy) = dragAmount
                                    totalDx += dx
                                    totalDy += dy
                                    change.consume()
                                }
                            }
                    ) {
                        AnimatedContent(
                            targetState = photo,
                            transitionSpec = {
                                if (isScrollMode) {
                                    val direction = if (lastScrollDirection >= 0) 1 else -1
                                    slideInVertically(
                                        animationSpec = tween(durationMillis = 200)
                                    ) { fullHeight -> fullHeight * direction } togetherWith
                                        slideOutVertically(
                                            animationSpec = tween(durationMillis = 200)
                                        ) { fullHeight -> -fullHeight * direction }
                                } else {
                                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                                }
                            },
                            label = "photoTransition",
                            modifier = Modifier.fillMaxSize()
                        ) { targetPhoto ->
                            AsyncImage(
                                model = targetPhoto?.uri,
                                contentDescription = stringResource(R.string.current_photo),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Gradient overlay for legibility using theme colors.
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val position = uiState.currentPosition ?: 0
                                val total = if (uiState.displayTotal > 0) uiState.displayTotal else uiState.photos.size
                                Text(
                                    text = stringResource(R.string.photo_position, position, total),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                if (queuedDeleteCount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Button(
                                            onClick = { showQueue = true },
                                            enabled = !uiState.mediaBusy,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(R.string.review_queue, queuedDeleteCount))
                                        }
                                    }
                                }
                                BoxWithConstraints(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val buttonSize = 56.dp
                                    val xOffsetLeft = this.maxWidth / 3 - buttonSize / 2
                                    val xOffsetRight = this.maxWidth * 2 / 3 - buttonSize / 2
                                    val xOffsetBack = (xOffsetLeft / 2) - (buttonSize / 2)

                                    IconButton(
                                        onClick = onUndo,
                                        enabled = !uiState.mediaBusy && uiState.lastAction != null,
                                        modifier = Modifier.offset(x = xOffsetBack)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(buttonSize)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = 0.65f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Undo,
                                                contentDescription = stringResource(R.string.undo),
                                                tint = Color.White.copy(alpha = if (uiState.lastAction != null) 1f else 0.45f)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = onDelete,
                                        enabled = !uiState.mediaBusy,
                                        modifier = Modifier
                                            .offset(x = xOffsetLeft)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(buttonSize)
                                                .clip(CircleShape)
                                                .background(Color.Red.copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.delete_label),
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = if (isScrollMode) onScrollForward else onKeep,
                                        enabled = !uiState.mediaBusy && (!isScrollMode || uiState.currentIndex < uiState.photos.lastIndex),
                                        modifier = Modifier
                                            .offset(x = xOffsetRight)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(buttonSize)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2E7D32).copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isScrollMode) Icons.Default.ArrowForward else Icons.Default.Check,
                                                contentDescription = stringResource(if (isScrollMode) R.string.next_photo else R.string.keep_label),
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            FloatingHeaderBar(
                onHome = onHome,
                onOpenSettings = onOpenSettings,
                onOpenTimeline = { showTimeline = true },
                timelineAvailable = !uiState.isLoading && uiState.loadError == null && uiState.timelinePhotos.isNotEmpty(),
                enabled = !uiState.mediaBusy
            )
            if (hasLimitedAccess) {
                Button(
                    onClick = onRequestPermissions,
                    enabled = !uiState.mediaBusy,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 52.dp)
                ) { Text(stringResource(R.string.manage_photo_access)) }
            }
        }
    }

    if (!uiState.offerResume && showTimeline && uiState.timelinePhotos.isNotEmpty()) {
        PhotoTimelineDialog(
            photos = uiState.timelinePhotos,
            currentPhoto = uiState.currentPhoto,
            onDismiss = { showTimeline = false },
            onSelect = { photo ->
                showTimeline = false
                onStartAt(photo)
            }
        )
    }

    if (!uiState.offerResume && showQueue && queuedDeleteCount > 0) {
        AlertDialog(
            onDismissRequest = { showQueue = false },
            title = { Text(stringResource(R.string.review_queue, queuedDeleteCount)) },
            text = {
                Column {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        itemsIndexed(uiState.queuedDeletes, key = { _, photo -> photo.uri.toString() }) { index, photo ->
                            val photoLabel = stringResource(R.string.photo_position, index + 1, queuedDeleteCount)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AsyncImage(photo.uri, null, Modifier.size(48.dp), contentScale = ContentScale.Crop)
                                Text(photoLabel, Modifier.weight(1f))
                                TextButton(onClick = { onRemoveFromQueue(photo) }) {
                                    Text(stringResource(R.string.keep_label))
                                }
                            }
                        }
                    }
                    TextButton(onClick = {
                        onDiscardQueue()
                        showQueue = false
                    }) { Text(stringResource(R.string.discard_queue)) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showQueue = false }) { Text(stringResource(R.string.done)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    showQueue = false
                    onCommitQueuedDeletes()
                }) {
                    Text(stringResource(if (supportsTrash) R.string.move_to_trash else R.string.delete_label))
                }
            }
        )
    } else if (showQueue) {
        LaunchedEffect(Unit) { showQueue = false }
    }

    if (!uiState.offerResume && showShareSheet && !isScrollMode) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.share_sheet_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = {
                        showShareSheet = false
                        onShare()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.share_photo))
                }
                TextButton(
                    onClick = { showShareSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }

    if (showWhatsNew) {
        AlertDialog(
            onDismissRequest = onDismissWhatsNew,
            confirmButton = {
                TextButton(onClick = onDismissWhatsNew) { Text(stringResource(R.string.done)) }
            },
            title = { Text("${stringResource(R.string.snapw_whats_new_title)} ${whatsNewVersionName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.snapw_whats_new_item_scroll_mode))
                    Text(stringResource(R.string.snapw_whats_new_item_languages))
                }
            }
        )
    }

    if (showInstructions) {
        AlertDialog(
            onDismissRequest = onDismissInstructions,
            confirmButton = {
                TextButton(onClick = onDismissInstructions) { Text(stringResource(R.string.done)) }
            },
            title = { Text(stringResource(R.string.instructions_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isScrollMode) {
                        Text(stringResource(R.string.instructions_delete))
                        Text(stringResource(R.string.instructions_share_scroll))
                        Text(stringResource(R.string.instructions_next_scroll))
                        Text(stringResource(R.string.instructions_prev_scroll))
                    } else {
                        Text(stringResource(R.string.instructions_delete))
                        Text(stringResource(R.string.instructions_keep))
                        Text(stringResource(R.string.instructions_share))
                    }
                    if (queuedDeleteCount > 0) {
                        Text(stringResource(R.string.instructions_queue))
                    }
                }
            }
        )
    }
}

@Composable
private fun BoxScope.FloatingHeaderBar(
    onHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTimeline: () -> Unit,
    timelineAvailable: Boolean,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 4.dp, start = 16.dp, end = 16.dp)
            .align(Alignment.TopCenter),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = 0.66f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.size(44.dp),
                onClick = onHome,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = stringResource(R.string.home)
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(
                modifier = Modifier.size(44.dp),
                onClick = onOpenTimeline,
                enabled = enabled && timelineAvailable
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = stringResource(R.string.photo_timeline)
                )
            }
            IconButton(
                modifier = Modifier.size(44.dp),
                onClick = onOpenSettings,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }
    }
}
