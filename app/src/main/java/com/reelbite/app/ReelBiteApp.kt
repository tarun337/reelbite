package com.reelbite.app

import android.app.Application
import android.content.Context
import com.reelbite.app.data.RecipeRepository
import com.reelbite.app.data.SettingsStore
import com.reelbite.app.data.VideoSaver
import com.reelbite.app.data.db.AppDatabase
import com.reelbite.app.data.remote.EmbedScraper
import com.reelbite.app.data.remote.InstagramClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/** Manual dependency container; the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = InstagramClient(http)

    val settings = SettingsStore(context)
    val repository = RecipeRepository(
        db = AppDatabase.create(context),
        client = client,
        scraper = EmbedScraper(context),
        saver = VideoSaver(context, client),
        thumbnailDir = File(context.filesDir, "thumbnails"),
        scope = appScope,
    )
}

class ReelBiteApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.repository.resumePendingWork()
    }
}
