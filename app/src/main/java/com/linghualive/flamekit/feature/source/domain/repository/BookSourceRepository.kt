package com.linghualive.flamekit.feature.source.domain.repository

import com.linghualive.flamekit.feature.source.domain.model.BookSource
import kotlinx.coroutines.flow.Flow

interface BookSourceRepository {
    fun getAllSources(): Flow<List<BookSource>>
    suspend fun getEnabledSources(): List<BookSource>
    suspend fun getSourceByUrl(sourceUrl: String): BookSource?
    suspend fun addSource(source: BookSource)
    suspend fun addSources(sources: List<BookSource>)
    suspend fun updateSource(source: BookSource)
    suspend fun deleteSource(source: BookSource)
    suspend fun getSourceCount(): Int
    suspend fun updateHealthStatus(sourceUrl: String, time: Long, success: Boolean)
    suspend fun deleteSources(sourceUrls: List<String>)
    suspend fun updateEnabledBatch(sourceUrls: List<String>, enabled: Boolean)
    suspend fun getAllSourcesOnce(): List<BookSource>
}
