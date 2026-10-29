package com.reelbite.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reelbite.app.ReelBiteApp
import com.reelbite.app.data.RecipeRepository
import com.reelbite.app.data.SettingsStore
import com.reelbite.app.data.db.DownloadState
import com.reelbite.app.data.db.GroupSummary
import com.reelbite.app.data.db.ReelEntity
import com.reelbite.app.data.db.ReelStatus
import com.reelbite.app.domain.InstagramLinks
import com.reelbite.app.domain.RecipeClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val loading: Boolean = true,
    val groups: List<GroupSummary> = emptyList(),
    val unsorted: GroupSummary? = null,
    val processing: List<ReelEntity> = emptyList(),
    val totalGroups: Int = 0,
    val totalReels: Int = 0,
    val savedReels: Int = 0,
) {
    val isEmpty: Boolean get() = !loading && totalReels == 0
}

class HomeViewModel(
    private val repository: RecipeRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    val query = MutableStateFlow("")

    val saveVideosByDefault: StateFlow<Boolean> = settings.saveVideosByDefault

    val state: StateFlow<HomeUiState> =
        combine(repository.groupSummaries, repository.allReels, query) { groups, reels, rawQuery ->
            val unsortedKey = RecipeClassifier.UNSORTED.key
            val q = rawQuery.trim().lowercase()
            val matchingGroupIds = if (q.isEmpty()) emptySet() else reels
                .filter { reel ->
                    listOfNotNull(reel.caption, reel.userTitle, reel.author).any { it.lowercase().contains(q) }
                }
                .mapNotNull { it.groupId }
                .toSet()
            val sorted = groups.filter { it.group.groupKey != unsortedKey }
            HomeUiState(
                loading = false,
                groups = if (q.isEmpty()) sorted else sorted.filter {
                    it.group.name.lowercase().contains(q) || it.group.id in matchingGroupIds
                },
                unsorted = groups.firstOrNull { it.group.groupKey == unsortedKey },
                processing = reels.filter { it.status == ReelStatus.PROCESSING },
                totalGroups = sorted.size,
                totalReels = reels.size,
                savedReels = reels.count { it.downloadState == DownloadState.DONE },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun linkCount(text: String): Int = InstagramLinks.extract(text).size

    suspend fun addReels(text: String, saveVideos: Boolean): RecipeRepository.AddResult {
        settings.setSaveVideosByDefault(saveVideos)
        return repository.addLinks(InstagramLinks.extract(text), saveVideos)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ReelBiteApp
                HomeViewModel(app.container.repository, app.container.settings)
            }
        }
    }
}
