package com.reelbite.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.reelbite.app.data.VideoSaver
import com.reelbite.app.ui.components.GradientButton

/**
 * Bottom sheet for adding one or many reel links at once.
 * Links can be pasted in any format — one per line, separated by spaces, or inside shared text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReelsSheet(
    initialText: String,
    saveVideosByDefault: Boolean,
    linkCount: (String) -> Int,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (text: String, saveVideos: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var text by rememberSaveable { mutableStateOf(initialText) }
    var saveVideos by rememberSaveable { mutableStateOf(saveVideosByDefault) }
    val count = remember(text) { linkCount(text) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Add reels", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Paste one or more Instagram reel links. We read each caption and file it under the right dish automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("https://www.instagram.com/reel/…") },
                minLines = 3,
                maxLines = 6,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                trailingIcon = if (text.isNotEmpty()) {
                    { IconButton(onClick = { text = "" }) { Icon(Icons.Rounded.Close, contentDescription = "Clear links") } }
                } else {
                    null
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {
                        val pasted = clipboard.getText()?.text.orEmpty()
                        if (pasted.isNotBlank()) text = if (text.isBlank()) pasted else "$text\n$pasted"
                    },
                    label = { Text("Paste") },
                    leadingIcon = { Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.weight(1f))
                val (label, color) = when {
                    text.isBlank() -> "" to MaterialTheme.colorScheme.onSurfaceVariant
                    count == 0 -> "No Instagram links found" to MaterialTheme.colorScheme.error
                    count == 1 -> "1 reel link found" to MaterialTheme.colorScheme.secondary
                    else -> "$count reel links found" to MaterialTheme.colorScheme.secondary
                }
                Text(label, style = MaterialTheme.typography.labelLarge, color = color)
            }

            Surface(
                onClick = { saveVideos = !saveVideos },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Save videos to phone", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Optional · stored in Movies/${VideoSaver.FOLDER}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = saveVideos,
                        onCheckedChange = { saveVideos = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                    )
                }
            }

            if (saveVideos) {
                Text(
                    "Only save reels you have the right to keep, for personal offline viewing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))
            GradientButton(
                text = when (count) {
                    0, 1 -> "Add & sort"
                    else -> "Add & sort $count reels"
                },
                icon = Icons.Rounded.AutoAwesome,
                enabled = count > 0,
                loading = busy,
                onClick = { onSubmit(text, saveVideos) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
