package com.linghualive.flamekit

import android.app.Application
import com.linghualive.flamekit.feature.source.domain.repository.BookSourceRepository
import com.linghualive.flamekit.feature.source.domain.model.BookSource
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltAndroidApp
class FlameKitApp : Application() {

    @Inject lateinit var bookSourceRepository: BookSourceRepository

    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate() {
        super.onCreate()
        loadDefaultSourcesIfEmpty()
    }

    private fun loadDefaultSourcesIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            if (bookSourceRepository.getSourceCount() == 0) {
                try {
                    val jsonStr = assets.open("default_sources.json")
                        .bufferedReader().use { it.readText() }
                    val sources = json.decodeFromString<List<BookSource>>(jsonStr)
                    bookSourceRepository.addSources(sources)
                } catch (_: Exception) {
                    // No default sources file or parse error
                }
            }
        }
    }
}
