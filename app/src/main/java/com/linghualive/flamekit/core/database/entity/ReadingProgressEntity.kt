package com.linghualive.flamekit.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class ReadingProgressEntity(
    @PrimaryKey
    val bookId: Long,
    val chapterIndex: Int = 0,
    val scrollPosition: Int = 0,
    val lastReadAt: Long = System.currentTimeMillis(),
)
