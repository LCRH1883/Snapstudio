package com.snapswipe.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.snapswipe.app.R
import com.snapswipe.app.data.PhotoItem
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle
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
    val photosByDate = remember(photos) {
        photos.sortedWith(compareBy<PhotoItem> { it.dateTaken ?: Long.MAX_VALUE }.thenBy { it.id })
    }
    val zone = ZoneId.systemDefault()
    val firstPhotoByDay = remember(photosByDate, zone) {
        buildMap {
            photosByDate.forEachIndexed { index, photo ->
                photo.dateTaken?.let { timestamp ->
                    val day = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
                    if (day !in this) put(day, index)
                }
            }
        }
    }
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(context, locale) { android.text.format.DateFormat.getMediumDateFormat(context) }
    val firstIndex = remember(photosByDate, currentPhoto) {
        photosByDate.indexOfFirst { it.uri == currentPhoto?.uri }.coerceAtLeast(0)
    }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = firstIndex)
    val scope = rememberCoroutineScope()
    var scrubPosition by remember { mutableStateOf<Float?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var jumpPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var jumpPending by rememberSaveable { mutableStateOf(false) }
    val visibleIndex by remember(gridState, photosByDate.size) {
        derivedStateOf { gridState.firstVisibleItemIndex.coerceAtMost(photosByDate.lastIndex) }
    }
    val jumpIndex = remember(photosByDate, jumpPhotoUri) {
        photosByDate.indexOfFirst { it.uri.toString() == jumpPhotoUri }
    }
    val jumpVisible by remember(gridState, jumpPhotoUri) {
        derivedStateOf { gridState.layoutInfo.visibleItemsInfo.any { it.key == jumpPhotoUri } }
    }
    val previewIndex = scrubPosition?.roundToInt()?.coerceIn(0, photosByDate.lastIndex)
        ?: if (jumpIndex >= 0 && jumpVisible) jumpIndex else visibleIndex
    val previewTimestamp = photosByDate[previewIndex].dateTaken
    val date = previewTimestamp?.let { dateFormat.format(Date(it)) }
    val previewLabel = date ?: stringResource(R.string.photo_position, previewIndex + 1, photosByDate.size)
    val scrubLabel = stringResource(R.string.timeline_scrubber)
    val itemAction = stringResource(R.string.start_here)

    if (showDatePicker && firstPhotoByDay.isNotEmpty()) {
        TimelineDatePicker(
            days = firstPhotoByDay.keys,
            initialYear = previewTimestamp?.let { Instant.ofEpochMilli(it).atZone(zone).year },
            onDismiss = { showDatePicker = false },
            onJump = { day ->
                val index = firstPhotoByDay.getValue(day)
                scrubPosition = null
                jumpPhotoUri = photosByDate[index].uri.toString()
                jumpPending = true
                showDatePicker = false
            }
        )
        return
    }

    LaunchedEffect(jumpIndex, jumpPending) {
        if (jumpPending) {
            if (jumpIndex >= 0) gridState.scrollToItem(jumpIndex)
            jumpPending = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.photo_timeline)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(previewLabel)
                if (photosByDate.size > 1) {
                    Slider(
                        value = scrubPosition ?: previewIndex.toFloat(),
                        onValueChange = {
                            jumpPhotoUri = null
                            scrubPosition = it
                        },
                        onValueChangeFinished = {
                            val target = scrubPosition?.roundToInt()?.coerceIn(0, photosByDate.lastIndex) ?: visibleIndex
                            scope.launch {
                                gridState.scrollToItem(target)
                                scrubPosition = null
                            }
                        },
                        valueRange = 0f..photosByDate.lastIndex.toFloat(),
                        modifier = Modifier.semantics {
                            contentDescription = scrubLabel
                            stateDescription = previewLabel
                        }
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(72.dp),
                    state = gridState,
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(photosByDate, key = { _, photo -> photo.uri.toString() }) { index, photo ->
                        val position = stringResource(R.string.photo_position, index + 1, photosByDate.size)
                        val taken = photo.dateTaken?.let { dateFormat.format(Date(it)) }
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = listOfNotNull(position, taken).joinToString(", "),
                            modifier = Modifier
                                .aspectRatio(1f)
                                .then(if (photo.uri.toString() == jumpPhotoUri) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                } else Modifier)
                                .clickable(onClickLabel = itemAction) { onSelect(photo) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        dismissButton = {
            if (firstPhotoByDay.isNotEmpty()) {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = stringResource(R.string.timeline_jump_to_date)
                    )
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

@Composable
private fun TimelineDatePicker(
    days: Set<LocalDate>,
    initialYear: Int?,
    onDismiss: () -> Unit,
    onJump: (LocalDate) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val numbers = remember(locale) { NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false } }
    val daysByYear = remember(days) { days.groupBy { it.year } }
    var selectedYear by rememberSaveable { mutableStateOf(initialYear) }
    var selectedMonth by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(null) }
    val year = selectedYear?.takeIf { it in daysByYear } ?: daysByYear.keys.first()
    val daysByMonth = remember(daysByYear, year) { daysByYear.getValue(year).groupBy { it.monthValue } }
    val month = selectedMonth?.takeIf { it in daysByMonth }
    val monthDays = daysByMonth[month].orEmpty()
    val day = monthDays.firstOrNull { it.dayOfMonth == selectedDay }
    val target = day ?: (monthDays.ifEmpty { daysByYear.getValue(year) }).first()
    val allMonths = stringResource(R.string.timeline_all_months)
    val allDays = stringResource(R.string.timeline_all_days)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.timeline_jump_to_date)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimelineDateField(
                    label = stringResource(R.string.timeline_year),
                    value = numbers.format(year),
                    options = daysByYear.keys.map { it to numbers.format(it) }
                ) {
                    selectedYear = it
                    selectedMonth = null
                    selectedDay = null
                }
                TimelineDateField(
                    label = stringResource(R.string.timeline_month),
                    value = month?.let { Month.of(it).getDisplayName(TextStyle.FULL_STANDALONE, locale) } ?: allMonths,
                    options = listOf(null to allMonths) + daysByMonth.keys.map {
                        it to Month.of(it).getDisplayName(TextStyle.FULL_STANDALONE, locale)
                    }
                ) {
                    selectedMonth = it
                    selectedDay = null
                }
                TimelineDateField(
                    label = stringResource(R.string.timeline_day),
                    value = day?.let { numbers.format(it.dayOfMonth) } ?: allDays,
                    options = listOf(null to allDays) + monthDays.map {
                        it.dayOfMonth to numbers.format(it.dayOfMonth)
                    },
                    enabled = month != null,
                    onSelect = { selectedDay = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onJump(target) }) { Text(stringResource(R.string.done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun TimelineDateField(
    label: String,
    value: String,
    options: List<Pair<Int?, String>>,
    enabled: Boolean = true,
    onSelect: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
                enabled = enabled
            ) {
                Text(value, Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 280.dp)
            ) {
                options.forEach { (key, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            expanded = false
                            onSelect(key)
                        }
                    )
                }
            }
        }
    }
}
