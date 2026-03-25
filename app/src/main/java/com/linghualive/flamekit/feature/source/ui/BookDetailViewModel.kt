package com.linghualive.flamekit.feature.source.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linghualive.flamekit.feature.bookshelf.domain.model.Book
import com.linghualive.flamekit.feature.bookshelf.domain.repository.BookRepository
import com.linghualive.flamekit.feature.bookshelf.domain.usecase.ImportBookUseCase
import com.linghualive.flamekit.feature.source.domain.model.BookDetail
import com.linghualive.flamekit.feature.source.domain.repository.BookSourceRepository
import com.linghualive.flamekit.feature.source.engine.SourceExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sourceRepository: BookSourceRepository,
    private val bookRepository: BookRepository,
    private val sourceExecutor: SourceExecutor,
    private val importBookUseCase: ImportBookUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val sourceUrl: String = savedStateHandle["sourceUrl"] ?: ""
    private val bookUrl: String = savedStateHandle["bookUrl"] ?: ""

    private val _detail = MutableStateFlow<BookDetail?>(null)
    val detail: StateFlow<BookDetail?> = _detail

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _addedToShelf = MutableStateFlow(false)
    val addedToShelf: StateFlow<Boolean> = _addedToShelf

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    // 0 = chapter, 1 = download
    private val _sourceType = MutableStateFlow(0)
    val sourceType: StateFlow<Int> = _sourceType

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val source = sourceRepository.getSourceByUrl(sourceUrl)
                    ?: throw Exception("书源不存在")
                _sourceType.value = source.sourceType
                val detail = sourceExecutor.getDetail(source, bookUrl)
                _detail.value = detail
            } catch (e: Exception) {
                _error.value = e.message ?: "加载失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToBookshelf() {
        val bookDetail = _detail.value ?: return

        if (_sourceType.value == 1) {
            downloadAndImport()
        } else {
            viewModelScope.launch {
                val book = Book(
                    title = bookDetail.name.ifBlank { "未知书名" },
                    author = bookDetail.author ?: "",
                    filePath = bookUrl,
                    format = "ONLINE",
                    coverPath = bookDetail.coverUrl,
                    sourceUrl = sourceUrl,
                )
                bookRepository.addBook(book)
                _addedToShelf.value = true
            }
        }
    }

    private fun downloadAndImport() {
        val bookDetail = _detail.value ?: return
        val downloadUrl = bookDetail.downloadUrl
        if (downloadUrl.isNullOrBlank()) {
            _error.value = "未找到下载链接，可能需要登录"
            return
        }

        viewModelScope.launch {
            _isDownloading.value = true
            _error.value = null
            try {
                val source = sourceRepository.getSourceByUrl(sourceUrl)
                val headers = source?.header?.let { parseHeaders(it) }

                val fileBytes = sourceExecutor.downloadFile(downloadUrl, headers)

                // Determine file extension from URL or default to epub
                val ext = downloadUrl.substringAfterLast(".", "epub")
                    .substringBefore("?")
                    .lowercase()
                    .let { if (it in listOf("epub", "pdf", "txt", "mobi")) it else "epub" }

                val fileName = "${bookDetail.name.take(50).replace(Regex("[/\\\\:*?\"<>|]"), "_")}.$ext"
                val booksDir = File(context.filesDir, "books").apply { mkdirs() }
                val destFile = File(booksDir, fileName)
                destFile.writeBytes(fileBytes)

                val uri = Uri.fromFile(destFile)
                importBookUseCase(uri)
                _addedToShelf.value = true
            } catch (e: Exception) {
                _error.value = "下载失败：${e.message}"
            } finally {
                _isDownloading.value = false
            }
        }
    }

    private fun parseHeaders(headerJson: String): Map<String, String>? {
        return try {
            val jsonObject = Json.decodeFromString<JsonObject>(headerJson)
            jsonObject.mapValues { it.value.jsonPrimitive.content }
        } catch (_: Exception) {
            null
        }
    }
}
