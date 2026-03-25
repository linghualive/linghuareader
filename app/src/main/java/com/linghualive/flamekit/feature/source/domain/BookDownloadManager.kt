package com.linghualive.flamekit.feature.source.domain

import android.content.Context
import android.net.Uri
import com.linghualive.flamekit.feature.bookshelf.domain.model.Book
import com.linghualive.flamekit.feature.bookshelf.domain.repository.BookRepository
import com.linghualive.flamekit.feature.source.domain.model.BookDetail
import com.linghualive.flamekit.feature.source.domain.repository.BookSourceRepository
import com.linghualive.flamekit.feature.source.engine.SourceExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val completed: Int,
    val total: Int,
)

@Singleton
class BookDownloadManager @Inject constructor(
    private val sourceRepository: BookSourceRepository,
    private val sourceExecutor: SourceExecutor,
    private val bookRepository: BookRepository,
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    val downloads: StateFlow<Map<Long, DownloadProgress>> = _downloads

    fun isDownloading(bookId: Long): Boolean = _downloads.value.containsKey(bookId)

    suspend fun startChapterDownload(bookDetail: BookDetail, sourceUrl: String): Long {
        val safeName = bookDetail.name.take(50).replace(Regex("[/\\\\:*?\"<>|]"), "_")
        val fileName = "${safeName}.txt"
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val destFile = File(booksDir, fileName)
        destFile.writeText("")

        val book = Book(
            title = bookDetail.name,
            author = bookDetail.author ?: "",
            filePath = Uri.fromFile(destFile).toString(),
            format = "txt",
            totalChapters = bookDetail.chapters.size,
        )
        val bookId = bookRepository.addBook(book)

        scope.launch {
            try {
                val source = sourceRepository.getSourceByUrl(sourceUrl) ?: return@launch
                val total = bookDetail.chapters.size
                val chapterPattern = Regex("^\\s*第[零一二三四五六七八九十百千万\\d]+[章节回集卷]")

                for ((i, chapter) in bookDetail.chapters.withIndex()) {
                    _downloads.update { it + (bookId to DownloadProgress(i + 1, total)) }

                    val title = if (chapterPattern.containsMatchIn(chapter.title)) {
                        chapter.title
                    } else {
                        "第${i + 1}章 ${chapter.title}"
                    }

                    try {
                        val content = sourceExecutor.getContent(source, chapter.url)
                        destFile.appendText("$title\n\n$content\n\n")
                    } catch (_: Exception) {
                        destFile.appendText("$title\n\n[加载失败]\n\n")
                    }
                }
            } finally {
                _downloads.update { it - bookId }
            }
        }

        return bookId
    }
}
