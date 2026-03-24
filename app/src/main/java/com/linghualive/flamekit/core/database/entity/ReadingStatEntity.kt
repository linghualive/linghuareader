package com.linghualive.flamekit.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_stats")
data class ReadingStatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val date: String,
    val durationSeconds: Long,
    val pagesRead: Int,
    val chaptersRead: Int,
)
