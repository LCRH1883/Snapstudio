@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.snapswipe.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.snapswipe.app.BuildConfig
import com.snapswipe.app.R
import com.snapswipe.app.data.DeleteMode
import com.snapswipe.app.data.InteractionMode
import com.snapswipe.app.data.SortOrder
import com.snapswipe.app.data.SortOrderPreferences
import com.snapswipe.app.data.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val sortOrderPreferences = remember { SortOrderPreferences(context) }
    val sortOrder by sortOrderPreferences.sortOrderFlow.collectAsState(initial = SortOrder.NEWEST_FIRST)
    val deleteMode by sortOrderPreferences.deleteModeFlow.collectAsState(initial = DeleteMode.IMMEDIATE)
    val interactionMode by sortOrderPreferences.interactionModeFlow.collectAsState(initial = InteractionMode.SWIPE_TO_CHOOSE)
    val themeMode by sortOrderPreferences.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    val coroutineScope = rememberCoroutineScope()
    val showAbout = remember { mutableStateOf(false) }
    val showWhatsNew = remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val supportEmail = stringResource(R.string.support_email)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = { Text(stringResource(R.string.settings_title)) },
                actions = {
                    TextButton(onClick = onDone) {
                        Text(stringResource(R.string.done))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.settings_section_appearance),
                subtitle = stringResource(R.string.settings_section_appearance_subtitle)
            ) {
                SegmentedSettingRow(
                    icon = Icons.Rounded.Brightness6,
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = stringResource(R.string.settings_theme_subtitle),
                    options = listOf(
                        SegmentedOption(ThemeMode.SYSTEM, stringResource(R.string.theme_system)),
                        SegmentedOption(ThemeMode.LIGHT, stringResource(R.string.theme_light)),
                        SegmentedOption(ThemeMode.DARK, stringResource(R.string.theme_dark))
                    ),
                    selected = themeMode,
                    onSelect = { mode ->
                        coroutineScope.launch { sortOrderPreferences.setThemeMode(mode) }
                    }
                )
            }

            SettingsSectionCard(
                title = stringResource(R.string.settings_section_review),
                subtitle = stringResource(R.string.settings_section_review_subtitle)
            ) {
                SegmentedSettingRow(
                    icon = Icons.Rounded.Sort,
                    title = stringResource(R.string.photo_review_order),
                    subtitle = stringResource(R.string.settings_sort_subtitle),
                    options = listOf(
                        SegmentedOption(SortOrder.NEWEST_FIRST, stringResource(R.string.newest_to_oldest)),
                        SegmentedOption(SortOrder.OLDEST_FIRST, stringResource(R.string.oldest_to_newest))
                    ),
                    selected = sortOrder,
                    onSelect = { order ->
                        coroutineScope.launch { sortOrderPreferences.setSortOrder(order) }
                    }
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                SegmentedSettingRow(
                    icon = Icons.Rounded.Gesture,
                    title = stringResource(R.string.snap_interaction_mode),
                    subtitle = stringResource(R.string.settings_interaction_subtitle),
                    options = listOf(
                        SegmentedOption(InteractionMode.SWIPE_TO_CHOOSE, stringResource(R.string.mode_swipe_to_choose)),
                        SegmentedOption(InteractionMode.SCROLL_AND_DELETE, stringResource(R.string.mode_scroll_and_delete))
                    ),
                    selected = interactionMode,
                    onSelect = { mode ->
                        coroutineScope.launch { sortOrderPreferences.setInteractionMode(mode) }
                    }
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                SwitchSettingRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = stringResource(R.string.delete_mode),
                    subtitle = stringResource(R.string.settings_delete_mode_subtitle),
                    checked = deleteMode == DeleteMode.QUEUED,
                    onCheckedChange = { isQueued ->
                        coroutineScope.launch {
                            sortOrderPreferences.setDeleteMode(
                                if (isQueued) DeleteMode.QUEUED else DeleteMode.IMMEDIATE
                            )
                        }
                    }
                )
            }

            SettingsSectionCard(
                title = stringResource(R.string.settings_section_support),
                subtitle = stringResource(R.string.settings_section_support_subtitle)
            ) {
                LinkSettingRow(
                    icon = Icons.Rounded.Star,
                    title = stringResource(R.string.snapw_whats_new_button),
                    subtitle = stringResource(R.string.settings_whats_new_subtitle),
                    onClick = { showWhatsNew.value = true }
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                LinkSettingRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.about_snap_swipe),
                    subtitle = stringResource(R.string.settings_about_subtitle),
                    onClick = { showAbout.value = true }
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                LinkSettingRow(
                    icon = Icons.Rounded.AlternateEmail,
                    title = stringResource(R.string.support),
                    subtitle = supportEmail,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$supportEmail")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showAbout.value) {
        AlertDialog(
            onDismissRequest = { showAbout.value = false },
            confirmButton = {
                TextButton(onClick = { showAbout.value = false }) {
                    Text(stringResource(R.string.done))
                }
            },
            title = { Text(stringResource(R.string.about_snap_swipe)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.supported_android_versions, "26+", "35"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.supported_languages_list),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        )
    }

    if (showWhatsNew.value) {
        AlertDialog(
            onDismissRequest = { showWhatsNew.value = false },
            confirmButton = {
                TextButton(onClick = { showWhatsNew.value = false }) {
                    Text(stringResource(R.string.done))
                }
            },
            title = { Text("${stringResource(R.string.snapw_whats_new_title)} ${BuildConfig.VERSION_NAME}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.snapw_whats_new_item_scroll_mode))
                    Text(stringResource(R.string.snapw_whats_new_item_languages))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun <T> SegmentedSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    options: List<SegmentedOption<T>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option.value == selected,
                    onClick = { onSelect(option.value) },
                    label = { Text(option.label) },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun SwitchSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LinkSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class SegmentedOption<T>(val value: T, val label: String)
