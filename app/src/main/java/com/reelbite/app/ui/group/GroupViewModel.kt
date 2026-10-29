package com.reelbite.app.ui.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reelbite.app.ReelBiteApp
import com.reelbite.app.data.RecipeRepository
import com.reelbite.app.data.db.DownloadState
import com.reelbite.app.data.db.RecipeGroupEntity
import com.reelbite.app.data.db.ReelEntity
import com.reelbite.app.domain.RecipeClassifier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GroupUiState(
    val loading: Boolean = true,
    val group: RecipeGroupEntity? = null,
    val reels: List<ReelEntity> = emptyList(),
    val progress: Map<Long, Float> = emptyMap(),
) {
    val isUnsorted: Boolean get() = group?.groupKey == RecipeClassifier.UNSORTED.key
    val savedCount: Int get() = reels.count { it.downloadState == DownloadState.DONE }
    val downloadableCount: Int
        get() = reels.count { it.downloadState == DownloadState.NONE || it.downloadState == DownloadState.FAILED }
}

class GroupViewModel(
    private val groupId: Long,
    private val repository: RecipeRepository,
) : ViewModel() {

    val state: StateFlow<GroupUiState> = combine(
        repository.observeGroup(groupId),
        repository.observeReels(groupId),
        repository.downloadProgress,
    ) { group, reels, progress ->
        GroupUiState(loading = false, group = group, reels = reels, progress = progress)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupUiState())

    suspend fun otherGroups(): List<RecipeGroupEntity> = repository.allGroups()

    fun rename(name: String) = viewModelScope.launch { repository.renameGroup(groupId, name) }
    fun deleteGroup(deleteVideos: Boolean) = viewModelScope.launch { repository.deleteGroup(groupId, deleteVideos) }
    fun downloadAll() = repository.downloadAll(groupId)

    fun setDishName(reelId: Long, name: String) = viewModelScope.launch { repository.setDishName(reelId, name) }
    fun moveReel(reelId: Long, toGroupId: Long) = viewModelScope.launch { repository.moveReel(reelId, toGroupId) }
    fun moveReelToNew(reelId: Long, name: String) = viewModelScope.launch { repository.moveReelToNewGroup(reelId, name) }
    fun deleteReel(reelId: Long, deleteVideo: Boolean) = viewModelScope.launch { repository.deleteReel(reelId, deleteVideo) }
    fun download(reelId: Long) = repository.requestDownload(reelId)

    /** Returns true if the saved video can be played; otherwise it was removed from the phone. */
    suspend fun verifySaved(reel: ReelEntity): Boolean = repository.verifySavedVideo(reel)

    companion object {
        const val ARG_GROUP_ID = "groupId"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ReelBiteApp
                val handle: SavedStateHandle = createSavedStateHandle()
                GroupViewModel(checkNotNull(handle.get<Long>(ARG_GROUP_ID)), app.container.repository)
            }
        }
    }
}
