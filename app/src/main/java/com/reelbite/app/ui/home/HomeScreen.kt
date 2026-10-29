package com.reelbite.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reelbite.app.data.db.GroupSummary
import com.reelbite.app.data.db.ReelEntity
import com.reelbite.app.ui.components.EmojiHalo
import com.reelbite.app.ui.components.FoodImage
import com.reelbite.app.ui.components.GradientButton
import com.reelbite.app.ui.components.Pill
import com.reelbite.app.ui.components.SectionLabel
import com.reelbite.app.ui.components.placeholderBrush
import com.reelbite.app.ui.theme.BrandGradient
import com.reelbite.app.ui.theme.BrandGradientHorizontal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sharedText: String?,
    onSharedTextConsumed: () -> Unit,
    onOpenGroup: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsState()
    val saveByDefault by viewModel.saveVideosByDefault.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showSheet by rememberSaveable { mutableStateOf(false) }
    var sheetInitialText by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    // Links shared from Instagram ("Share → ReelBite") open the sheet pre-filled.
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            sheetInitialText = sharedText
            showSheet = true
            onSharedTextConsumed()
        }
    }

    fun openSheet() {
        sheetInitialText = ""
        showSheet = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!state.isEmpty) {
                GradientButton(
                    text = "Add reels",
                    icon = Icons.Rounded.Add,
                    onClick = ::openSheet,
                    height = 56.dp,
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.loading -> Unit
                state.isEmpty -> EmptyHome(onAdd = ::openSheet)
                else -> GroupsGrid(
                    state = state,
                    query = query,
                    onQueryChange = { viewModel.query.value = it },
                    onOpenGroup = onOpenGroup,
                )
            }
        }
    }

    if (showSheet) {
        AddReelsSheet(
            initialText = sheetInitialText,
            saveVideosByDefault = saveByDefault,
            linkCount = viewModel::linkCount,
            busy = busy,
            onDismiss = { showSheet = false },
            onSubmit = { text, save ->
                scope.launch {
                    busy = true
                    val result = viewModel.addReels(text, save)
                    busy = false
                    showSheet = false
                    val message = buildString {
                        when (result.added) {
                            0 -> append("No new reels")
                            1 -> append("Sorting 1 reel…")
                            else -> append("Sorting ${result.added} reels…")
                        }
                        if (result.duplicates > 0) append(" · ${result.duplicates} already in your cookbook")
                    }
                    snackbarHostState.showSnackbar(message)
                }
            },
        )
    }
}

@Composable
private fun HomeHeader(state: HomeUiState) {
    Column(Modifier.padding(top = 20.dp, bottom = 4.dp)) {
        Text(
            buildAnnotatedString {
                append("Reel")
                withStyle(SpanStyle(brush = BrandGradient)) { append("Bite") }
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            buildString {
                append(plural(state.totalGroups, "dish", "dishes"))
                append(" · ")
                append(plural(state.totalReels, "reel", "reels"))
                if (state.savedReels > 0) append(" · ${state.savedReels} saved offline")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupsGrid(
    state: HomeUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenGroup: (Long) -> Unit,
) {
    val full = GridItemSpan(2)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { full }, key = "header") { HomeHeader(state) }

        item(span = { full }, key = "search") {
            SearchField(query = query, onQueryChange = onQueryChange)
        }

        if (state.processing.isNotEmpty()) {
            item(span = { full }, key = "processing") { ProcessingStrip(state.processing) }
        }

        state.unsorted?.let { unsorted ->
            item(span = { full }, key = "unsorted") {
                UnsortedBanner(unsorted, onClick = { onOpenGroup(unsorted.group.id) })
            }
        }

        if (state.groups.isNotEmpty()) {
            item(span = { full }, key = "label") {
                SectionLabel(
                    if (query.isBlank()) "Your recipe groups" else "Matching groups",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(state.groups, key = { it.group.id }) { summary ->
                GroupCard(summary, onClick = { onOpenGroup(summary.group.id) })
            }
        } else if (query.isNotBlank()) {
            item(span = { full }, key = "no-results") {
                Text(
                    "No dishes match “$query”",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search dishes, captions, creators") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear search") } }
        } else {
            null
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

@Composable
private fun ProcessingStrip(reels: List<ReelEntity>) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Reading & sorting ${plural(reels.size, "reel", "reels")}…",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(reels, key = { it.id }) { reel ->
                    Box(
                        Modifier
                            .size(width = 54.dp, height = 72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(placeholderBrush(reel.shortcode)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnsortedBanner(summary: GroupSummary, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${plural(summary.reelCount, "reel needs", "reels need")} a dish name",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "We couldn't tell what's cooking. Tap to name them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun GroupCard(summary: GroupSummary, onClick: () -> Unit) {
    val group = summary.group
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.semantics {
            contentDescription = "${group.name}, ${plural(summary.reelCount, "reel", "reels")}"
        },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f),
        ) {
            FoodImage(
                path = summary.coverPath,
                emoji = group.emoji,
                seed = group.groupKey,
                emojiSize = 48,
                modifier = Modifier.fillMaxSize(),
            )
            // Scrim so the title stays readable on any thumbnail.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
            ) {
                Pill(text = summary.reelCount.toString(), icon = Icons.Rounded.Link)
                if (summary.savedCount > 0) {
                    Pill(text = summary.savedCount.toString(), icon = Icons.Rounded.DownloadDone)
                }
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
            ) {
                Text(group.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(2.dp))
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    plural(summary.reelCount, "reel", "reels"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun EmptyHome(onAdd: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.weight(1f))
        EmojiHalo("🍳", size = 140.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            buildAnnotatedString {
                append("Your reels, ")
                withStyle(SpanStyle(brush = BrandGradientHorizontal)) { append("sorted by dish") }
            },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Drop in food reels from Instagram. Two biryani reels and a chicken fry? You'll get a Chicken Biryani group and a Chicken Fry group — automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        HowItWorks()
        Spacer(Modifier.height(28.dp))
        GradientButton(
            text = "Add your first reels",
            icon = Icons.Rounded.Add,
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1.2f))
    }
}

@Composable
private fun HowItWorks() {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Step(Icons.Rounded.Share, "In Instagram, tap Share → ReelBite, or copy the reel link")
            Step(Icons.Rounded.AutoAwesome, "We read the caption and file it under the right dish")
            Step(Icons.Rounded.DownloadDone, "Optionally save the video to your phone for offline cooking")
        }
    }
}

@Composable
private fun Step(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

internal fun plural(count: Int, one: String, many: String) = if (count == 1) "1 $one" else "$count $many"
