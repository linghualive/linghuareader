package com.linghualive.flamekit.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "source_subscriptions")
data class SourceSubscriptionEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val lastUpdate: Long = 0,
    val enabled: Boolean = true,
    val sourceCount: Int = 0,
)
