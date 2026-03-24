package com.linghualive.flamekit.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String = "",
    val filePath: String,
    val format: String,
    val coverPath: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long? = null,
    val totalChapters: Int = 0,
)
