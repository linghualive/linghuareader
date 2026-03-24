package com.linghualive.flamekit.feature.sync.domain.model

import com.linghualive.flamekit.core.datastore.ReadingPreferences
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val books: List<BookDto>,
    val bookmarks: List<BookmarkDto>,
    val readingProgress: List<ReadingProgressDto>,
    val bookSources: List<BookSourceDto>,
    val readingPrefs: ReadingPreferences,
    val backupTime: Long,
    val appVersion: String,
)

@Serializable
data class BookDto(
    val id: Long = 0,
    val title: String,
    val author: String = "",
    val filePath: String,
    val format: String,
    val coverPath: String? = null,
    val addedAt: Long = 0,
    val lastReadAt: Long? = null,
    val totalChapters: Int = 0,
)

@Serializable
data class BookmarkDto(
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val position: Int,
    val title: String = "",
    val createdAt: Long = 0,
)

@Serializable
data class ReadingProgressDto(
    val bookId: Long,
    val chapterIndex: Int = 0,
    val scrollPosition: Int = 0,
    val lastReadAt: Long = 0,
)

@Serializable
data class BookSourceDto(
    val sourceUrl: String,
    val sourceName: String,
    val sourceGroup: String? = null,
    val sourceType: Int = 0,
    val enabled: Boolean = true,
    val header: String? = null,
    val loginUrl: String? = null,
    val lastUpdateTime: Long = 0,
    val searchUrl: String? = null,
    val searchList: String? = null,
    val searchName: String? = null,
    val searchAuthor: String? = null,
    val searchCover: String? = null,
    val searchBookUrl: String? = null,
    val detailName: String? = null,
    val detailAuthor: String? = null,
    val detailCover: String? = null,
    val detailIntro: String? = null,
    val detailTocUrl: String? = null,
    val tocList: String? = null,
    val tocName: String? = null,
    val tocUrl: String? = null,
    val contentRule: String? = null,
    val contentNextUrl: String? = null,
)
