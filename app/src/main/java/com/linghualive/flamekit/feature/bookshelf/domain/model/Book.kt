package com.linghualive.flamekit.feature.bookshelf.domain.model

data class Book(
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
