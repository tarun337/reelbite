package com.reelbite.app.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reelbite.app.data.db.RecipeGroupEntity
import com.reelbite.app.data.db.ReelEntity
import com.reelbite.app.ui.components.DeleteDialog
import com.reelbite.app.ui.components.FoodImage
import com.reelbite.app.ui.components.GradientButton
import com.reelbite.app.ui.components.MoveToGroupDialog
import com.reelbite.app.ui.components.TextInputDialog
import com.reelbite.app.ui.home.plural
import com.reelbite.app.ui.openInInstagram
import com.reelbite.app.ui.playSavedVideo
import kotlinx.coroutines.launch

private sealed interface GroupDialog {
    data object RenameGroup : GroupDialog
    data object DeleteGroup : GroupDialog
    data class NameDish(val reel: ReelEntity) : GroupDialog
    data class Move(val reel: ReelEntity, val groups: List<RecipeGroupEntity>) : GroupDialog
    data class DeleteReel(val reel: ReelEntity) : GroupDialog
}

@Composable
fun GroupScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: GroupViewModel = viewModel(factory = GroupViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<GroupDialog?>(null) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    // The group disappears when its last reel is moved/removed, or when it's merged by a rename.
    LaunchedEffect(state.loading, state.group) {
        if (!state.loading && state.group == null) onBack()
    }
    val group = state.group ?: return

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "hero") {
                GroupHero(
                    group = group,
                    coverPath = state.reels.firstNotNullOfOrNull { it.thumbnailPath },
                    subtitle = buildString {
                        append(plural(state.reels.size, "reel", "reels"))
                        if (state.savedCount > 0) append(" · ${state.savedCount} saved offline")
                    },
                    onBack = onBack,
                    menu = {
                        Box {
                            IconButton(
                                onClick = { menuOpen = true },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                            ) { Icon(Icons.Rounded.MoreVert, contentDescription = "Group actions", tint = Color.White) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                if (!state.isUnsorted) {
                                    DropdownMenuItem(
                                        text = { Text("Rename group") },
                                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                        onClick = { menuOpen = false; dialog = GroupDialog.RenameGroup },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Delete group", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { menuOpen = false; dialog = GroupDialog.DeleteGroup },
                                )
                            }
                        }
                    },
                )
            }

            if (state.downloadableCount > 0) {
                item(key = "save-all") {
                    GradientButton(
                        text = if (state.downloadableCount == state.reels.size) "Save all to phone"
                        else "Save ${state.downloadableCount} remaining to phone",
                        icon = Icons.Rounded.Download,
                        height = 48.dp,
                        onClick = {
                            viewModel.downloadAll()
                            scope.launch { snackbarHostState.showSnackbar("Saving videos to Movies/ReelBite…") }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }

            items(state.reels, key = { it.id }) { reel ->
                ReelCard(
                    reel = reel,
                    groupEmoji = group.emoji,
                    progress = state.progress[reel.id],
                    onOpen = { openInInstagram(context, reel.url) },
                    onPlay = {
                        scope.launch {
                            if (viewModel.verifySaved(reel)) {
                                reel.localVideoUri?.let { playSavedVideo(context, it) }
                            } else {
                                snackbarHostState.showSnackbar("That video was removed from your phone")
                            }
                        }
                    },
                    onDownload = { viewModel.download(reel.id) },
                    onNameDish = { dialog = GroupDialog.NameDish(reel) },
                    onMove = {
                        scope.launch { dialog = GroupDialog.Move(reel, viewModel.otherGroups()) }
                    },
                    onDelete = { dialog = GroupDialog.DeleteReel(reel) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        GroupDialog.RenameGroup -> TextInputDialog(
            title = "Rename group",
            label = "Group name",
            initial = group.name,
            confirmText = "Rename",
            supportingText = "Using an existing group's name merges them",
            onDismiss = { dialog = null },
            onConfirm = { viewModel.rename(it); dialog = null },
        )

        GroupDialog.DeleteGroup -> DeleteDialog(
            title = "Delete “${group.name}”?",
            message = "This removes ${plural(state.reels.size, "reel", "reels")} from ReelBite. Reels on Instagram aren't affected.",
            savedVideoCount = state.savedCount,
            onDismiss = { dialog = null },
            onConfirm = { deleteVideos ->
                dialog = null
                viewModel.deleteGroup(deleteVideos)
            },
        )

        is GroupDialog.NameDish -> TextInputDialog(
            title = "What's cooking?",
            label = "Dish name",
            initial = d.reel.userTitle.orEmpty(),
            confirmText = "Sort",
            supportingText = "e.g. Chicken Biryani, Paneer Tikka. We'll file it in the right group.",
            onDismiss = { dialog = null },
            onConfirm = { viewModel.setDishName(d.reel.id, it); dialog = null },
        )

        is GroupDialog.Move -> MoveToGroupDialog(
            groups = d.groups,
            currentGroupId = d.reel.groupId,
            onDismiss = { dialog = null },
            onMoveToGroup = { viewModel.moveReel(d.reel.id, it); dialog = null },
            onMoveToNewGroup = { viewModel.moveReelToNew(d.reel.id, it); dialog = null },
        )

        is GroupDialog.DeleteReel -> DeleteDialog(
            title = "Remove this reel?",
            message = "It will be removed from “${group.name}”.",
            savedVideoCount = if (d.reel.localVideoUri != null) 1 else 0,
            onDismiss = { dialog = null },
            onConfirm = { deleteVideo ->
                dialog = null
                viewModel.deleteReel(d.reel.id, deleteVideo)
            },
        )
    }
}

@Composable
private fun GroupHero(
    group: RecipeGroupEntity,
    coverPath: String?,
    subtitle: String,
    onBack: () -> Unit,
    menu: @Composable () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(260.dp),
    ) {
        FoodImage(
            path = coverPath,
            emoji = group.emoji,
            seed = group.groupKey,
            emojiSize = 72,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.35f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.background,
                    ),
                ),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            menu()
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(group.emoji, style = MaterialTheme.typography.headlineSmall) }
            Spacer(Modifier.height(10.dp))
            Text(
                group.name,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
