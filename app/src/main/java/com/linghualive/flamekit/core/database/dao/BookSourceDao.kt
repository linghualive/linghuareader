package com.linghualive.flamekit.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.linghualive.flamekit.core.database.entity.BookSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookSourceDao {

    @Query("SELECT * FROM book_sources ORDER BY sourceName ASC")
    fun getAllSources(): Flow<List<BookSourceEntity>>

    @Query("SELECT * FROM book_sources WHERE enabled = 1 ORDER BY sourceName ASC")
    suspend fun getEnabledSources(): List<BookSourceEntity>

    @Query("SELECT * FROM book_sources WHERE sourceUrl = :sourceUrl")
    suspend fun getSourceByUrl(sourceUrl: String): BookSourceEntity?

    @Query("SELECT * FROM book_sources WHERE sourceGroup = :group ORDER BY sourceName ASC")
    fun getSourcesByGroup(group: String): Flow<List<BookSourceEntity>>

    @Query("SELECT * FROM book_sources WHERE sourceName LIKE '%' || :query || '%' ORDER BY sourceName ASC")
    fun searchSources(query: String): Flow<List<BookSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: BookSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<BookSourceEntity>)

    @Update
    suspend fun updateSource(source: BookSourceEntity)

    @Delete
    suspend fun deleteSource(source: BookSourceEntity)

    @Query("DELETE FROM book_sources")
    suspend fun deleteAllSources()

    @Query("SELECT COUNT(*) FROM book_sources")
    suspend fun getSourceCount(): Int

    @Query("SELECT * FROM book_sources ORDER BY sourceName ASC")
    suspend fun getAllSourcesOnce(): List<BookSourceEntity>

    @Query("UPDATE book_sources SET lastCheckTime = :time, lastCheckSuccess = :success WHERE sourceUrl = :sourceUrl")
    suspend fun updateHealthStatus(sourceUrl: String, time: Long, success: Boolean)

    @Query("DELETE FROM book_sources WHERE sourceUrl IN (:sourceUrls)")
    suspend fun deleteSources(sourceUrls: List<String>)

    @Query("UPDATE book_sources SET enabled = :enabled WHERE sourceUrl IN (:sourceUrls)")
    suspend fun updateEnabledBatch(sourceUrls: List<String>, enabled: Boolean)
}
