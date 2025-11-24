package com.example.snapswipe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    onUndo: () -> Unit = {}
) {
    val hasPhotos = uiState.photos.isNotEmpty()
    val atEnd = uiState.isAtEnd
    val canUndo = uiState.lastAction != null
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

                atEnd -> {
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
                                            onShare()
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

                        // Gradient overlay for legibility.
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.6f),
                                            Color.Black.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Photo ${uiState.currentIndex + 1} of ${uiState.photos.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Swipe right to keep, left to delete, up to share",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Button(
                                        onClick = onUndo,
                                        enabled = canUndo
                                    ) {
                                        Text("Undo")
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = onDelete,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Delete")
                                    }
                                    Button(
                                        onClick = onKeep,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Keep")
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
