package com.snapswipe.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.snapswipe.app.data.SortOrder
import com.snapswipe.app.data.SortOrderPreferences
import com.snapswipe.app.data.DeleteMode
import com.snapswipe.app.data.InteractionMode
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import com.snapswipe.app.BuildConfig
import androidx.compose.ui.res.stringResource
import com.snapswipe.app.R
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.os.LocaleListCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDone: () -> Unit
    // Later: pass callbacks for sort order changes if needed to reload photos.
) {
    val context = LocalContext.current
    val sortOrderPreferences = androidx.compose.runtime.remember { SortOrderPreferences(context) }
    val sortOrder by sortOrderPreferences.sortOrderFlow.collectAsState(initial = SortOrder.NEWEST_FIRST)
    val deleteMode by sortOrderPreferences.deleteModeFlow.collectAsState(initial = DeleteMode.IMMEDIATE)
    val interactionMode by sortOrderPreferences.interactionModeFlow.collectAsState(initial = InteractionMode.SWIPE_TO_CHOOSE)
    val coroutineScope = rememberCoroutineScope()
    val showAbout = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.photo_review_order),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                SettingsRadioOption(
                    label = stringResource(R.string.newest_to_oldest),
                    selected = sortOrder == SortOrder.NEWEST_FIRST,
                    onSelect = {
                        coroutineScope.launch { sortOrderPreferences.setSortOrder(SortOrder.NEWEST_FIRST) }
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.oldest_to_newest),
                    selected = sortOrder == SortOrder.OLDEST_FIRST,
                    onSelect = {
                        coroutineScope.launch { sortOrderPreferences.setSortOrder(SortOrder.OLDEST_FIRST) }
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.delete_mode),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                SettingsRadioOption(
                    label = stringResource(R.string.delete_mode_immediate),
                    selected = deleteMode == DeleteMode.IMMEDIATE,
                    onSelect = {
                        coroutineScope.launch { sortOrderPreferences.setDeleteMode(DeleteMode.IMMEDIATE) }
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.delete_mode_queued),
                    selected = deleteMode == DeleteMode.QUEUED,
                    onSelect = {
                        coroutineScope.launch { sortOrderPreferences.setDeleteMode(DeleteMode.QUEUED) }
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.snap_interaction_mode),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                SettingsRadioOption(
                    label = stringResource(R.string.mode_swipe_to_choose),
                    selected = interactionMode == InteractionMode.SWIPE_TO_CHOOSE,
                    onSelect = {
                        coroutineScope.launch { sortOrderPreferences.setInteractionMode(InteractionMode.SWIPE_TO_CHOOSE) }
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.mode_scroll_and_delete),
                    selected = interactionMode == InteractionMode.SCROLL_AND_DELETE,
                    onSelect = {
                        coroutineScope.launch { sortOrderPreferences.setInteractionMode(InteractionMode.SCROLL_AND_DELETE) }
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_snap_swipe),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = stringResource(R.string.about_snap_swipe_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.support),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                val email = stringResource(R.string.support_email)
                val annotated = buildAnnotatedString {
                    val start = length
                    append(email)
                    addStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        start,
                        start + email.length
                    )
                    addStringAnnotation(tag = "email", annotation = email, start = start, end = start + email.length)
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ClickableText(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium,
                        onClick = { offset ->
                            annotated.getStringAnnotations(tag = "email", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:${annotation.item}")
                                    }
                                    context.startActivity(intent)
                                }
                        }
                    )
                    TextButton(onClick = { showAbout.value = true }) {
                        Text(stringResource(R.string.about_snap_swipe))
                    }
                }
            }
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

}

@Composable
private fun SettingsRadioOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
