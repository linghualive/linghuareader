package com.linghualive.flamekit.feature.source.data.repository

import com.linghualive.flamekit.core.database.dao.BookSourceDao
import com.linghualive.flamekit.feature.source.data.mapper.toDomain
import com.linghualive.flamekit.feature.source.data.mapper.toEntity
import com.linghualive.flamekit.feature.source.domain.model.BookSource
import com.linghualive.flamekit.feature.source.domain.repository.BookSourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BookSourceRepositoryImpl @Inject constructor(
    private val bookSourceDao: BookSourceDao,
) : BookSourceRepository {

    override fun getAllSources(): Flow<List<BookSource>> =
        bookSourceDao.getAllSources().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getEnabledSources(): List<BookSource> =
        bookSourceDao.getEnabledSources().map { it.toDomain() }

    override suspend fun getSourceByUrl(sourceUrl: String): BookSource? =
        bookSourceDao.getSourceByUrl(sourceUrl)?.toDomain()

    override suspend fun addSource(source: BookSource) =
        bookSourceDao.insertSource(source.toEntity())

    override suspend fun addSources(sources: List<BookSource>) =
        bookSourceDao.insertSources(sources.map { it.toEntity() })

    override suspend fun updateSource(source: BookSource) =
        bookSourceDao.updateSource(source.toEntity())

    override suspend fun deleteSource(source: BookSource) =
        bookSourceDao.deleteSource(source.toEntity())

    override suspend fun getSourceCount(): Int =
        bookSourceDao.getSourceCount()

    override suspend fun updateHealthStatus(sourceUrl: String, time: Long, success: Boolean) =
        bookSourceDao.updateHealthStatus(sourceUrl, time, success)

    override suspend fun deleteSources(sourceUrls: List<String>) =
        bookSourceDao.deleteSources(sourceUrls)

    override suspend fun updateEnabledBatch(sourceUrls: List<String>, enabled: Boolean) =
        bookSourceDao.updateEnabledBatch(sourceUrls, enabled)

    override suspend fun getAllSourcesOnce(): List<BookSource> =
        bookSourceDao.getAllSourcesOnce().map { it.toDomain() }
}
