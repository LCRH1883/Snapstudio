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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.snapswipe.app.R
import com.snapswipe.app.data.InteractionMode
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
    val processedAll = !hasPhotos && uiState.lastAction != null && !uiState.isLoading
    var showShareSheet by remember { mutableStateOf(false) }
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
    LaunchedEffect(processedAll, queuedDeleteCount) {
        if (processedAll && queuedDeleteCount > 0) {
            onCommitQueuedDeletes()
        }
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onHome) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = stringResource(R.string.home)
                        )
                    }
                },
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        }
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

                uiState.errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRequestPermissions) {
                            Text(stringResource(R.string.check_permissions))
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onRestart) { Text(stringResource(R.string.restart)) }
                            Button(onClick = onOpenSettings) { Text(stringResource(R.string.settings)) }
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
                            .pointerInput(uiState.currentIndex, interactionMode) {
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
                                contentDescription = "Current photo",
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
                                modifier = Modifier.fillMaxWidth(),
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
                                            onClick = onCommitQueuedDeletes,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(R.string.delete_queued_count, queuedDeleteCount))
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
                                                contentDescription = "Back",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = onDelete,
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
                                                imageVector = Icons.Default.Check,
                                                contentDescription = stringResource(R.string.keep_label),
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
        }
    }

    if (showShareSheet && !isScrollMode) {
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
            title = { Text(stringResource(R.string.whats_new_title, whatsNewVersionName)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.whats_new_item_scroll_mode))
                    Text(stringResource(R.string.whats_new_item_languages))
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
