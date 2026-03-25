package com.linghualive.flamekit.feature.bookshelf.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linghualive.flamekit.feature.bookshelf.domain.model.Book
import com.linghualive.flamekit.feature.bookshelf.domain.usecase.DeleteBookUseCase
import com.linghualive.flamekit.feature.bookshelf.domain.usecase.GetBooksUseCase
import com.linghualive.flamekit.feature.bookshelf.domain.usecase.ImportBookUseCase
import com.linghualive.flamekit.feature.source.domain.BookDownloadManager
import com.linghualive.flamekit.feature.source.domain.DownloadProgress
import com.linghualive.flamekit.feature.update.data.AppUpdateChecker
import com.linghualive.flamekit.feature.update.domain.model.AppRelease
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder {
    RECENT,
    TITLE,
    ADDED,
}

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    getBooksUseCase: GetBooksUseCase,
    private val importBookUseCase: ImportBookUseCase,
    private val deleteBookUseCase: DeleteBookUseCase,
    private val appUpdateChecker: AppUpdateChecker,
    private val downloadManager: BookDownloadManager,
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.RECENT)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val books: StateFlow<List<Book>> = combine(
        getBooksUseCase(),
        _sortOrder,
    ) { books, order ->
        when (order) {
            SortOrder.RECENT -> books.sortedByDescending { it.lastReadAt ?: 0L }
            SortOrder.TITLE -> books.sortedBy { it.title }
            SortOrder.ADDED -> books.sortedByDescending { it.addedAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<Map<Long, DownloadProgress>> = downloadManager.downloads

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _availableUpdate = MutableStateFlow<AppRelease?>(null)
    val availableUpdate: StateFlow<AppRelease?> = _availableUpdate.asStateFlow()

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            try {
                _availableUpdate.value = appUpdateChecker.checkForUpdate()
            } catch (_: Exception) {
                // Silently ignore update check failures
            }
        }
    }

    fun dismissUpdate() {
        _availableUpdate.value = null
    }

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                importBookUseCase(uri)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            deleteBookUseCase(book)
        }
    }
}
