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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainSwipeScreen(
    uiState: SnapSwipeUiState,
    onOpenSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
    onUndo: () -> Unit = {},
    onStartFromBeginning: () -> Unit = {},
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
    val showPhoto = hasPhotos && !uiState.isLoading && uiState.loadError == null
    val processedAll = !hasPhotos && uiState.totalCount > 0 && !uiState.isLoading
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showTimeline by rememberSaveable { mutableStateOf(false) }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }
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
        snackbarHost = {
            SnackbarHost(
                snackbar,
                Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            val compactHeight = maxHeight < 400.dp
            val fitPhoto = compactHeight || maxWidth >= 600.dp
            if (showPhoto) {
                val photo = uiState.currentPhoto
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(uiState.currentPhoto?.uri, interactionMode, uiState.mediaBusy, uiState.deleteMode) {
                            if (uiState.mediaBusy) return@pointerInput
                            val horizontalThreshold = 60f
                            val verticalThreshold = 80f
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
                            contentScale = if (fitPhoto) ContentScale.Fit else ContentScale.Crop
                        )
                    }

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
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                            )
                            .padding(horizontal = 16.dp, vertical = if (compactHeight) 8.dp else 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val position = uiState.currentPosition ?: 0
                            val total = uiState.displayTotal.takeIf { it > 0 } ?: uiState.photos.size
                            Text(
                                text = stringResource(R.string.photo_position, position, total),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            if (queuedDeleteCount > 0) {
                                Button(
                                    onClick = { showQueue = true },
                                    enabled = !uiState.mediaBusy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.review_queue, queuedDeleteCount))
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                // Keep the controls aligned with the physical swipe directions.
                                horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledIconButton(
                                    onClick = onDelete,
                                    enabled = !uiState.mediaBusy,
                                    modifier = Modifier.size(56.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color(0xFFFF8178),
                                        contentColor = Color(0xFF340A05),
                                        disabledContainerColor = InactivePhotoControl,
                                        disabledContentColor = OnInactivePhotoControl
                                    )
                                ) {
                                    Icon(Icons.Default.Close, stringResource(R.string.delete_label))
                                }
                                FilledIconButton(
                                    onClick = if (isScrollMode) onScrollForward else onKeep,
                                    enabled = !uiState.mediaBusy && (!isScrollMode || uiState.currentIndex < uiState.photos.lastIndex),
                                    modifier = Modifier.size(56.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color(0xFF7BD890),
                                        contentColor = Color(0xFF092B15),
                                        disabledContainerColor = InactivePhotoControl,
                                        disabledContentColor = OnInactivePhotoControl
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isScrollMode) Icons.Default.ArrowForward else Icons.Default.Check,
                                        contentDescription = stringResource(if (isScrollMode) R.string.next_photo else R.string.keep_label)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    )
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingHeaderBar(
                    onUndo = onUndo,
                    canUndo = uiState.lastAction != null && !uiState.isLoading && uiState.loadError == null,
                    onOpenSettings = onOpenSettings,
                    onOpenTimeline = { showTimeline = true },
                    onStartFromBeginning = onStartFromBeginning,
                    timelineAvailable = !uiState.isLoading && uiState.loadError == null && uiState.timelinePhotos.isNotEmpty(),
                    enabled = !uiState.mediaBusy
                )
                if (hasLimitedAccess) {
                    Button(
                        onClick = onRequestPermissions,
                        enabled = !uiState.mediaBusy,
                        modifier = Modifier.padding(top = 6.dp)
                    ) { Text(stringResource(R.string.manage_photo_access)) }
                }
                if (!showPhoto) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                            .widthIn(max = 480.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when {
                                uiState.isLoading -> {
                                    CircularProgressIndicator()
                                    Text(stringResource(R.string.loading_photos))
                                }
                                uiState.loadError != null -> {
                                    Text(
                                        text = stringResource(uiState.loadError),
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                    val permissionError = uiState.loadError == R.string.permission_denied_message
                                    Button(onClick = if (permissionError) onRequestPermissions else onReload) {
                                        Text(stringResource(if (permissionError) R.string.check_permissions else R.string.refresh))
                                    }
                                }
                                else -> {
                                    Text(
                                        text = stringResource(if (processedAll) R.string.reviewed_run_title else R.string.no_photos_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = stringResource(if (processedAll) R.string.reviewed_run_subtitle else R.string.no_photos_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    if (processedAll && queuedDeleteCount > 0) {
                                        Button(onClick = { showQueue = true }, enabled = !uiState.mediaBusy) {
                                            Text(stringResource(R.string.review_queue, queuedDeleteCount))
                                        }
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                                    ) {
                                        Button(
                                            onClick = if (processedAll) onRestart else onReload,
                                            enabled = !uiState.mediaBusy
                                        ) {
                                            Text(stringResource(if (processedAll) R.string.restart else R.string.refresh))
                                        }
                                        Button(onClick = onOpenSettings, enabled = !uiState.mediaBusy) {
                                            Text(stringResource(R.string.settings))
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
                    LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 320.dp)) {
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
                    .verticalScroll(rememberScrollState())
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
                    Text(stringResource(R.string.snapw_whats_new_item_review))
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

private val InactivePhotoControl = Color(0xFF40434A)
private val OnInactivePhotoControl = Color(0xFFB6BAC4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloatingHeaderBar(
    onUndo: () -> Unit,
    canUndo: Boolean,
    onOpenSettings: () -> Unit,
    onOpenTimeline: () -> Unit,
    onStartFromBeginning: () -> Unit,
    timelineAvailable: Boolean,
    enabled: Boolean
) {
    var showOptions by remember { mutableStateOf(false) }
    val colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val title = stringResource(R.string.app_name)
    val titleStyle = MaterialTheme.typography.titleMedium
    val titleWidth = rememberTextMeasurer().measure(
        text = AnnotatedString(title),
        style = titleStyle,
        softWrap = false,
        maxLines = 1
    ).size.width
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = 0.66f))
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        val availableTitleWidth = maxWidth - (48.dp * 4) - 8.dp
        val titleFits = with(LocalDensity.current) { titleWidth.toDp() } <= availableTitleWidth
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onUndo,
                    enabled = enabled && canUndo,
                    colors = colors
                ) {
                    Icon(Icons.Default.Undo, stringResource(R.string.undo))
                }
                if (titleFits) {
                    Text(
                        text = title,
                        style = titleStyle,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                FilledIconButton(
                    onClick = onOpenTimeline,
                    enabled = enabled && timelineAvailable,
                    colors = colors
                ) {
                    Icon(Icons.Default.DateRange, stringResource(R.string.photo_timeline))
                }
                FilledIconButton(
                    onClick = onOpenSettings,
                    enabled = enabled,
                    colors = colors
                ) {
                    Icon(Icons.Default.Settings, stringResource(R.string.settings))
                }
                Box {
                    FilledIconButton(
                        onClick = { showOptions = true },
                        enabled = enabled && timelineAvailable,
                        colors = colors
                    ) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.review_options))
                    }
                    DropdownMenu(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.start_from_beginning)) },
                            onClick = {
                                showOptions = false
                                onStartFromBeginning()
                            }
                        )
                    }
                }
            }
            if (!titleFits) {
                Text(
                    text = title,
                    style = titleStyle,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
