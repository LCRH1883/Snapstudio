package com.example.snapswipe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSwipeScreen(
    uiState: SnapSwipeUiState,
    onOpenSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit = {},
    onRestart: () -> Unit = {},
    onReload: () -> Unit = {},
    queuedDeleteCount: Int = 0,
    onCommitQueuedDeletes: () -> Unit = {}
) {
    val hasPhotos = uiState.photos.isNotEmpty()
    val processedAll = !hasPhotos && uiState.lastAction != null && !uiState.isLoading
    var showShareSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(processedAll, queuedDeleteCount) {
        if (processedAll && queuedDeleteCount > 0) {
            onCommitQueuedDeletes()
        }
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Snap Swipe") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
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
                        Text("Loading photos...")
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
                            Text("Check permissions")
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
                            text = "You’ve reviewed all photos in this run.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Restart or adjust settings to change the order.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onRestart) { Text("Restart") }
                            Button(onClick = onOpenSettings) { Text("Settings") }
                        }
                    }
                }

                !hasPhotos -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No photos found.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Add photos to your device and refresh.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onReload) { Text("Refresh") }
                            Button(onClick = onOpenSettings) { Text("Settings") }
                        }
                    }
                }

                hasPhotos -> {
                    val photo = uiState.currentPhoto
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(uiState.currentIndex) {
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
                                                totalDx > 60 -> onKeep()
                                                totalDx < -60 -> onDelete()
                                            }
                                        } else if (totalDy < -80) {
                                            showShareSheet = true
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
                        AsyncImage(
                            model = photo?.uri,
                            contentDescription = "Current photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

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
                                val total = if (uiState.totalCount > 0) uiState.totalCount else uiState.photos.size
                                Text(
                                    text = "Photo $position of $total",
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
                                            Text("Delete queued ($queuedDeleteCount)")
                                        }
                                    }
                                }
                                BoxWithConstraints(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val buttonSize = 56.dp
                                    val xOffsetLeft = this.maxWidth / 3 - buttonSize / 2
                                    val xOffsetRight = this.maxWidth * 2 / 3 - buttonSize / 2
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
                                                contentDescription = "Delete",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = onKeep,
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
                                                contentDescription = "Keep",
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

    if (showShareSheet) {
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
                    text = "Share this photo",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = {
                        showShareSheet = false
                        onShare()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share photo")
                }
                TextButton(
                    onClick = { showShareSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
