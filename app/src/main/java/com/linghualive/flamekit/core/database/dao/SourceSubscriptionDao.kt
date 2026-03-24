package com.linghualive.flamekit.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.linghualive.flamekit.core.database.entity.SourceSubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceSubscriptionDao {

    @Query("SELECT * FROM source_subscriptions ORDER BY name ASC")
    fun getAll(): Flow<List<SourceSubscriptionEntity>>

    @Query("SELECT * FROM source_subscriptions WHERE enabled = 1")
    suspend fun getEnabled(): List<SourceSubscriptionEntity>

    @Query("SELECT * FROM source_subscriptions WHERE url = :url")
    suspend fun getByUrl(url: String): SourceSubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SourceSubscriptionEntity)

    @Update
    suspend fun update(subscription: SourceSubscriptionEntity)

    @Query("DELETE FROM source_subscriptions WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Delete
    suspend fun delete(subscription: SourceSubscriptionEntity)
}
