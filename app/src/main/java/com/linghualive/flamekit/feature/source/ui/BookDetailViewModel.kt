package com.linghualive.flamekit.feature.source.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linghualive.flamekit.feature.bookshelf.domain.model.Book
import com.linghualive.flamekit.feature.bookshelf.domain.repository.BookRepository
import com.linghualive.flamekit.feature.source.domain.model.BookDetail
import com.linghualive.flamekit.feature.source.domain.repository.BookSourceRepository
import com.linghualive.flamekit.feature.source.engine.SourceExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sourceRepository: BookSourceRepository,
    private val bookRepository: BookRepository,
    private val sourceExecutor: SourceExecutor,
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
