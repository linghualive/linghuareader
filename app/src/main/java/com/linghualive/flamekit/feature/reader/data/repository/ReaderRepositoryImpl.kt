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
import com.linghualive.flamekit.feature.source.domain.repository.BookSourceRepository
import com.linghualive.flamekit.feature.source.engine.SourceExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val readingProgressDao: ReadingProgressDao,
    private val parserFactory: BookParserFactory,
    private val sourceExecutor: SourceExecutor,
    private val bookSourceRepository: BookSourceRepository,
    @ApplicationContext private val context: Context,
) : ReaderRepository {

    private val chapterCache = mutableMapOf<Long, List<Chapter>>()

    override suspend fun loadChapters(bookId: Long): List<ChapterInfo> {
        val chapters = getOrParseChapters(bookId)
        return chapters.map { ChapterInfo(index = it.index, title = it.title) }
    }

    override suspend fun loadChapterContent(bookId: Long, chapterIndex: Int): String {
        val book = bookDao.getBookById(bookId) ?: return ""

        if (book.format == "ONLINE") {
            val chapters = getOrParseChapters(bookId)
            val chapter = chapters.getOrNull(chapterIndex) ?: return ""
            // For online books, chapter.content stores the URL; fetch real content
            val chapterUrl = chapter.content
            if (chapterUrl.startsWith("http")) {
                val source = book.sourceUrl?.let { bookSourceRepository.getSourceByUrl(it) }
                    ?: return "书源不存在"
                return try {
                    sourceExecutor.getContent(source, chapterUrl)
                } catch (e: Exception) {
                    "加载失败：${e.message}"
                }
            }
            return chapter.content
        }

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
        chapterCache[bookId]?.let { return it }

        val book = bookDao.getBookById(bookId) ?: return emptyList()

        if (book.format == "ONLINE") {
            val source = book.sourceUrl?.let { bookSourceRepository.getSourceByUrl(it) }
                ?: return emptyList()
            val bookUrl = book.filePath  // filePath stores the book URL for online books
            return try {
                val detail = sourceExecutor.getDetail(source, bookUrl)
                val chapters = detail.chapters.mapIndexed { index, ch ->
                    Chapter(
                        index = index,
                        title = ch.title,
                        content = ch.url,  // Store URL in content for lazy loading
                        startOffset = 0,
                    )
                }
                chapterCache[bookId] = chapters
                chapters
            } catch (e: Exception) {
                emptyList()
            }
        }

        val uri = Uri.parse(book.filePath)
        val parser = parserFactory.getParser(book.filePath)
        val result = parser.parse(context, uri)
        chapterCache[bookId] = result.chapters
        return result.chapters
    }
}
