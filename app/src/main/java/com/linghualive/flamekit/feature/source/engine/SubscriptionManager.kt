package com.linghualive.flamekit.feature.source.engine

import com.linghualive.flamekit.core.database.dao.SourceSubscriptionDao
import com.linghualive.flamekit.core.database.entity.SourceSubscriptionEntity
import com.linghualive.flamekit.feature.source.domain.model.BookSource
import com.linghualive.flamekit.feature.source.domain.model.SourceSubscription
import com.linghualive.flamekit.feature.source.domain.repository.BookSourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val bookSourceRepository: BookSourceRepository,
    private val subscriptionDao: SourceSubscriptionDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun refreshSubscription(subscription: SourceSubscription): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(subscription.url).build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))

                val sources = json.decodeFromString<List<BookSource>>(body)
                bookSourceRepository.addSources(sources)

                val now = System.currentTimeMillis()
                subscriptionDao.update(
                    SourceSubscriptionEntity(
                        url = subscription.url,
                        name = subscription.name,
                        lastUpdate = now,
                        enabled = subscription.enabled,
                        sourceCount = sources.size,
                    )
                )

                Result.success(sources.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun refreshAll(): Result<Map<String, Int>> =
        withContext(Dispatchers.IO) {
            try {
                val subscriptions = subscriptionDao.getEnabled()
                val results = mutableMapOf<String, Int>()
                for (entity in subscriptions) {
                    val sub = entity.toDomain()
                    val result = refreshSubscription(sub)
                    result.onSuccess { count -> results[sub.name] = count }
                }
                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun addSubscription(name: String, url: String): Result<SourceSubscription> =
        withContext(Dispatchers.IO) {
            try {
                val subscription = SourceSubscription(name = name, url = url)
                subscriptionDao.insert(subscription.toEntity())
                val refreshResult = refreshSubscription(subscription)
                val updated = subscriptionDao.getByUrl(url)?.toDomain()
                    ?: subscription
                Result.success(updated)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun removeSubscription(url: String) {
        subscriptionDao.deleteByUrl(url)
    }

    fun getSubscriptions(): Flow<List<SourceSubscription>> =
        subscriptionDao.getAll().map { entities -> entities.map { it.toDomain() } }

    private fun SourceSubscriptionEntity.toDomain() = SourceSubscription(
        name = name,
        url = url,
        lastUpdate = lastUpdate,
        enabled = enabled,
        sourceCount = sourceCount,
    )

    private fun SourceSubscription.toEntity() = SourceSubscriptionEntity(
        url = url,
        name = name,
        lastUpdate = lastUpdate,
        enabled = enabled,
        sourceCount = sourceCount,
    )
}
