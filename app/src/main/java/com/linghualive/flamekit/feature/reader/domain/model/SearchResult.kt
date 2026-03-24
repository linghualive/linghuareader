package com.linghualive.flamekit.feature.reader.domain.model

data class SearchResult(
    val chapterIndex: Int,
    val chapterTitle: String,
    val matchText: String,
    val matchPosition: Int,
    val keyword: String,
)
