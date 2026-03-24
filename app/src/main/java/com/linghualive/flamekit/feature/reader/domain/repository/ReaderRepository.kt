package com.linghualive.flamekit.feature.reader.domain.repository

import com.linghualive.flamekit.feature.reader.domain.model.ChapterInfo

interface ReaderRepository {
    suspend fun loadChapters(bookId: Long): List<ChapterInfo>
    suspend fun loadChapterContent(bookId: Long, chapterIndex: Int): String
    suspend fun saveProgress(bookId: Long, chapterIndex: Int, scrollPosition: Int)
    suspend fun getProgress(bookId: Long): Pair<Int, Int>?
    suspend fun getBookTitle(bookId: Long): String
    suspend fun getBookFormat(bookId: Long): String
    suspend fun getBookFilePath(bookId: Long): String
}
