package com.reelbite.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tiny preference store; remembers the last "save videos to phone" choice. */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _saveVideosByDefault = MutableStateFlow(prefs.getBoolean(KEY_SAVE_VIDEOS, false))
    val saveVideosByDefault: StateFlow<Boolean> = _saveVideosByDefault.asStateFlow()

    fun setSaveVideosByDefault(value: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_VIDEOS, value).apply()
        _saveVideosByDefault.value = value
    }

    private companion object {
        const val KEY_SAVE_VIDEOS = "save_videos_by_default"
    }
}
