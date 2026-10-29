package com.reelbite.app.ui.group

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reelbite.app.data.db.DownloadState
import com.reelbite.app.data.db.ReelEntity
import com.reelbite.app.data.db.ReelStatus
import com.reelbite.app.ui.components.FoodImage
import com.reelbite.app.ui.components.Pill
import com.reelbite.app.ui.theme.Mint

@Composable
fun ReelCard(
    reel: ReelEntity,
    groupEmoji: String,
    progress: Float?,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onNameDish: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val title = reel.userTitle ?: reel.caption?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()
    val needsInfo = reel.status == ReelStatus.NEEDS_INFO

    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.animateContentSize(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row {
                Box(
                    Modifier
                        .size(width = 92.dp, height = 132.dp)
                        .clip(RoundedCornerShape(14.dp)),
                ) {
                    FoodImage(
                        path = reel.thumbnailPath,
                        emoji = groupEmoji,
                        seed = reel.shortcode,
                        emojiSize = 30,
                        contentDescription = "Reel thumbnail",
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (reel.downloadState == DownloadState.DONE) {
                        Pill(
                            text = "Saved",
                            container = Mint.copy(alpha = 0.9f),
                            content = Color(0xFF00281B),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(6.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            reel.author?.let {
                                Text(
                                    "@$it",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(2.dp))
                            }
                            Text(
                                title ?: "Instagram reel",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More actions")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Open in Instagram") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) },
                                    onClick = { menuOpen = false; onOpen() },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (reel.userTitle == null) "Name the dish" else "Rename dish") },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                    onClick = { menuOpen = false; onNameDish() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Move to group") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.DriveFileMove, null) },
                                    onClick = { menuOpen = false; onMove() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { menuOpen = false; onDelete() },
                                )
                            }
                        }
                    }
                    reel.caption?.let { caption ->
                        val body = caption.trim().removePrefix(title.orEmpty()).trim()
                        if (body.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    DownloadAction(reel, progress, onPlay, onDownload)
                }
            }

            if (needsInfo) {
                Spacer(Modifier.height(12.dp))
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            reel.errorMessage ?: "Name this dish to sort it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalButton(onClick = onNameDish) { Text("Name it") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadAction(reel: ReelEntity, progress: Float?, onPlay: () -> Unit, onDownload: () -> Unit) {
    when (reel.downloadState) {
        DownloadState.DONE -> FilledTonalButton(onClick = onPlay) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Play offline")
        }

        DownloadState.QUEUED, DownloadState.DOWNLOADING -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        reel.downloadState == DownloadState.QUEUED -> "Waiting to save…"
                        progress != null && progress > 0f -> "Saving… ${(progress * 100).toInt()}%"
                        else -> "Finding video…"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (progress != null && progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50)),
                )
            }
        }

        DownloadState.FAILED -> Column {
            OutlinedButton(onClick = onDownload) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry save")
            }
            reel.downloadError?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        DownloadState.NONE -> OutlinedButton(onClick = onDownload) {
            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save to phone")
        }
    }
}
