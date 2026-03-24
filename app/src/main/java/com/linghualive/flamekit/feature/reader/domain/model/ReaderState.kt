package com.linghualive.flamekit.feature.reader.domain.model

data class ReaderState(
    val bookId: Long = 0,
    val bookTitle: String = "",
    val bookFormat: String = "txt",
    val bookFilePath: String = "",
    val chapters: List<ChapterInfo> = emptyList(),
    val currentChapterIndex: Int = 0,
    val currentChapterContent: String = "",
    val scrollPosition: Int = 0,
    val isLoading: Boolean = true,
    val showToolbar: Boolean = false,
    val showChapterList: Boolean = false,
    val showSettingsPanel: Boolean = false,
    val showTtsPanel: Boolean = false,
    val isAutoPageTurning: Boolean = false,
    val showSearchSheet: Boolean = false,
    val showNoteList: Boolean = false,
    val showAddNoteDialog: Boolean = false,
    val isSearching: Boolean = false,
    val selectedTextForNote: String = "",
    val selectedTextStartPos: Int = 0,
    val selectedTextEndPos: Int = 0,
)

data class ChapterInfo(
    val index: Int,
    val title: String,
)

data class Bookmark(
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val position: Int,
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
