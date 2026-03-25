package com.linghualive.flamekit.feature.source.domain.model

data class SearchResult(
    val bookName: String,
    val author: String?,
    val coverUrl: String?,
    val bookUrl: String,
    val intro: String? = null,
    val sourceName: String,
    val sourceUrl: String,
)

data class BookDetail(
    val name: String,
    val author: String?,
    val coverUrl: String?,
    val intro: String?,
    val tocUrl: String?,
    val chapters: List<OnlineChapter>,
    val downloadUrl: String? = null,
)

data class OnlineChapter(
    val title: String,
    val url: String,
)
