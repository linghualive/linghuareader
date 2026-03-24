package com.linghualive.flamekit.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.linghualive.flamekit.core.database.entity.ReadingStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingStatDao {
    @Query("SELECT * FROM reading_stats WHERE date = :date")
    suspend fun getStatsByDate(date: String): List<ReadingStatEntity>

    @Query("SELECT * FROM reading_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date")
    fun getStatsBetween(startDate: String, endDate: String): Flow<List<ReadingStatEntity>>

    @Query("SELECT SUM(durationSeconds) FROM reading_stats")
    suspend fun getTotalDuration(): Long?

    @Query("SELECT SUM(durationSeconds) FROM reading_stats WHERE date = :date")
    suspend fun getDailyDuration(date: String): Long?

    @Query("SELECT DISTINCT date FROM reading_stats ORDER BY date DESC")
    fun getReadingDates(): Flow<List<String>>

    @Upsert
    suspend fun upsert(stat: ReadingStatEntity)

    @Query("SELECT COUNT(DISTINCT date) FROM reading_stats WHERE durationSeconds > 0")
    suspend fun getTotalReadingDays(): Int
}
