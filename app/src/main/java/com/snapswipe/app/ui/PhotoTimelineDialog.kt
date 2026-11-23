package com.snapswipe.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.snapswipe.app.R
import com.snapswipe.app.data.PhotoItem
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun PhotoTimelineDialog(
    photos: List<PhotoItem>,
    currentPhoto: PhotoItem?,
    onDismiss: () -> Unit,
    onSelect: (PhotoItem) -> Unit
) {
    if (photos.isEmpty()) return
    val context = LocalContext.current
    val dateFormat = remember(context) { android.text.format.DateFormat.getMediumDateFormat(context) }
    val firstIndex = remember(photos, currentPhoto) {
        photos.indexOfFirst { it.uri == currentPhoto?.uri }.coerceAtLeast(0)
    }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = firstIndex)
    val scope = rememberCoroutineScope()
    var scrubPosition by remember { mutableStateOf<Float?>(null) }
    val visibleIndex = gridState.firstVisibleItemIndex.coerceAtMost(photos.lastIndex)
    val previewIndex = scrubPosition?.roundToInt()?.coerceIn(0, photos.lastIndex) ?: visibleIndex
    val date = photos[previewIndex].dateTaken?.let { dateFormat.format(Date(it)) }
    val scrubLabel = stringResource(R.string.timeline_scrubber)
    val itemAction = stringResource(R.string.start_here)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.photo_timeline)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(date ?: stringResource(R.string.photo_position, previewIndex + 1, photos.size))
                if (photos.size > 1) {
                    Slider(
                        value = scrubPosition ?: visibleIndex.toFloat(),
                        onValueChange = { scrubPosition = it },
                        onValueChangeFinished = {
                            val target = scrubPosition?.roundToInt()?.coerceIn(0, photos.lastIndex) ?: visibleIndex
                            scope.launch {
                                gridState.scrollToItem(target)
                                scrubPosition = null
                            }
                        },
                        valueRange = 0f..photos.lastIndex.toFloat(),
                        modifier = Modifier.semantics { contentDescription = scrubLabel }
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(72.dp),
                    state = gridState,
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(photos, key = { _, photo -> photo.uri.toString() }) { index, photo ->
                        val position = stringResource(R.string.photo_position, index + 1, photos.size)
                        val taken = photo.dateTaken?.let { dateFormat.format(Date(it)) }
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = listOfNotNull(position, taken).joinToString(", "),
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable(onClickLabel = itemAction) { onSelect(photo) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
