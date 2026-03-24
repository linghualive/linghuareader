package com.linghualive.flamekit.feature.reader.domain.model

data class Note(
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val startPosition: Int,
    val endPosition: Int,
    val selectedText: String,
    val noteContent: String?,
    val highlightColor: Long,
    val createdAt: Long = System.currentTimeMillis(),
)
