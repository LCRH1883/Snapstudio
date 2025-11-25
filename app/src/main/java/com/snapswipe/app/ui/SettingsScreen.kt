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
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import com.snapswipe.app.BuildConfig

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
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                actions = {
                    TextButton(onClick = onDone) {
                        Text("Done")
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
                    text = "Photo review order",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                SortOrderOption(
                    label = "Newest to oldest",
                    selected = sortOrder == SortOrder.NEWEST_FIRST,
                    onSelect = {
                        coroutineScope.launch { sortOrderPreferences.setSortOrder(SortOrder.NEWEST_FIRST) }
                    }
                )
                SortOrderOption(
                    label = "Oldest to newest",
                    selected = sortOrder == SortOrder.OLDEST_FIRST,
                    onSelect = {
                        coroutineScope.launch { sortOrderPreferences.setSortOrder(SortOrder.OLDEST_FIRST) }
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Delete mode",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                SortOrderOption(
                    label = "Immediate (confirm per delete if required)",
                    selected = deleteMode == DeleteMode.IMMEDIATE,
                    onSelect = {
                        coroutineScope.launch { sortOrderPreferences.setDeleteMode(DeleteMode.IMMEDIATE) }
                    }
                )
                SortOrderOption(
                    label = "Queue deletions and confirm once",
                    selected = deleteMode == DeleteMode.QUEUED,
                    onSelect = {
                        coroutineScope.launch { sortOrderPreferences.setDeleteMode(DeleteMode.QUEUED) }
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "About Snap Swipe",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Clean up your photos by swiping to keep, delete, or share.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Support",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                val email = "support@snapswipe.xyz"
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
            }
        }
    }
}

@Composable
private fun SortOrderOption(
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
