package com.linghualive.flamekit.feature.source.engine

import com.linghualive.flamekit.feature.source.domain.model.BookSource
import com.linghualive.flamekit.feature.source.domain.repository.BookSourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceHealthChecker @Inject constructor(
    private val sourceExecutor: SourceExecutor,
    private val bookSourceRepository: BookSourceRepository,
) {
    data class HealthResult(
        val sourceUrl: String,
        val sourceName: String,
        val isHealthy: Boolean,
        val responseTime: Long,
        val error: String?,
        val checkedAt: Long = System.currentTimeMillis(),
    )

    suspend fun checkSource(source: BookSource): HealthResult {
        return try {
            val startTime = System.currentTimeMillis()
            val results = sourceExecutor.search(source, "test")
            val elapsed = System.currentTimeMillis() - startTime
            val healthy = results.isNotEmpty()

            bookSourceRepository.updateHealthStatus(
                sourceUrl = source.sourceUrl,
                time = System.currentTimeMillis(),
                success = healthy,
            )

            HealthResult(
                sourceUrl = source.sourceUrl,
                sourceName = source.sourceName,
                isHealthy = healthy,
                responseTime = elapsed,
                error = if (results.isEmpty()) "无搜索结果" else null,
            )
        } catch (e: Exception) {
            bookSourceRepository.updateHealthStatus(
                sourceUrl = source.sourceUrl,
                time = System.currentTimeMillis(),
                success = false,
            )

            HealthResult(
                sourceUrl = source.sourceUrl,
                sourceName = source.sourceName,
                isHealthy = false,
                responseTime = -1,
                error = e.message,
            )
        }
    }

    fun checkAllSources(): Flow<HealthResult> = flow {
        val sources = bookSourceRepository.getEnabledSources()
        val semaphore = Semaphore(5)

        // Process in batches of 5 for concurrency control
        val batchSize = 5
        for (batch in sources.chunked(batchSize)) {
            val results = coroutineScope {
                batch.map { source ->
                    async {
                        semaphore.acquire()
                        try {
                            checkSource(source)
                        } finally {
                            semaphore.release()
                        }
                    }
                }.awaitAll()
            }
            for (result in results) {
                emit(result)
            }
        }
    }

    suspend fun autoDisableUnhealthySources(threshold: Int = 3) {
        val allSources = bookSourceRepository.getAllSourcesOnce()
        val toDisable = allSources.filter { source ->
            !source.lastCheckSuccess && source.lastCheckTime > 0 && source.enabled
        }
        if (toDisable.isNotEmpty()) {
            bookSourceRepository.updateEnabledBatch(
                toDisable.map { it.sourceUrl },
                enabled = false,
            )
        }
    }
}
