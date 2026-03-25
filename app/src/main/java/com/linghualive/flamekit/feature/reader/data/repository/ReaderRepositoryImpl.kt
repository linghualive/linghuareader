package com.linghualive.flamekit.feature.reader.data.repository

import android.content.Context
import android.net.Uri
import com.linghualive.flamekit.core.database.dao.BookDao
import com.linghualive.flamekit.core.database.dao.ReadingProgressDao
import com.linghualive.flamekit.core.database.entity.ReadingProgressEntity
import com.linghualive.flamekit.feature.reader.domain.model.ChapterInfo
import com.linghualive.flamekit.feature.reader.domain.repository.ReaderRepository
import com.linghualive.flamekit.feature.reader.format.BookParserFactory
import com.linghualive.flamekit.feature.reader.format.Chapter
import com.linghualive.flamekit.feature.source.domain.BookDownloadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val readingProgressDao: ReadingProgressDao,
    private val parserFactory: BookParserFactory,
    private val downloadManager: BookDownloadManager,
    @ApplicationContext private val context: Context,
) : ReaderRepository {

    private val chapterCache = mutableMapOf<Long, List<Chapter>>()

    override suspend fun loadChapters(bookId: Long): List<ChapterInfo> {
        val chapters = getOrParseChapters(bookId)
        return chapters.map { ChapterInfo(index = it.index, title = it.title) }
    }

    override suspend fun loadChapterContent(bookId: Long, chapterIndex: Int): String {
        val chapters = getOrParseChapters(bookId)
        return chapters.getOrNull(chapterIndex)?.content ?: ""
    }

    override suspend fun saveProgress(bookId: Long, chapterIndex: Int, scrollPosition: Int) {
        readingProgressDao.upsertProgress(
            ReadingProgressEntity(
                bookId = bookId,
                chapterIndex = chapterIndex,
                scrollPosition = scrollPosition,
            )
        )
        bookDao.updateLastReadAt(bookId)
    }

    override suspend fun getProgress(bookId: Long): Pair<Int, Int>? {
        val progress = readingProgressDao.getProgress(bookId) ?: return null
        return progress.chapterIndex to progress.scrollPosition
    }

    override suspend fun getBookTitle(bookId: Long): String {
        return bookDao.getBookById(bookId)?.title ?: ""
    }

    override suspend fun getBookFormat(bookId: Long): String {
        return bookDao.getBookById(bookId)?.format ?: "txt"
    }

    override suspend fun getBookFilePath(bookId: Long): String {
        return bookDao.getBookById(bookId)?.filePath ?: ""
    }

    private suspend fun getOrParseChapters(bookId: Long): List<Chapter> {
        // Skip cache for books still downloading so we get latest chapters
        if (!downloadManager.isDownloading(bookId)) {
            chapterCache[bookId]?.let { return it }
        }

        val book = bookDao.getBookById(bookId) ?: return emptyList()
        val uri = Uri.parse(book.filePath)
        val parser = parserFactory.getParser(book.filePath)
        val result = parser.parse(context, uri)
        chapterCache[bookId] = result.chapters
        return result.chapters
    }
}
