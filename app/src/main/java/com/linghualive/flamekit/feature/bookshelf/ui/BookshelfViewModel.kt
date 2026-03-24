package com.linghualive.flamekit.feature.bookshelf.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linghualive.flamekit.feature.bookshelf.domain.model.Book
import com.linghualive.flamekit.feature.bookshelf.domain.usecase.DeleteBookUseCase
import com.linghualive.flamekit.feature.bookshelf.domain.usecase.GetBooksUseCase
import com.linghualive.flamekit.feature.bookshelf.domain.usecase.ImportBookUseCase
import com.linghualive.flamekit.feature.update.data.AppUpdateChecker
import com.linghualive.flamekit.feature.update.domain.model.AppRelease
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    getBooksUseCase: GetBooksUseCase,
    private val importBookUseCase: ImportBookUseCase,
    private val deleteBookUseCase: DeleteBookUseCase,
    private val appUpdateChecker: AppUpdateChecker,
) : ViewModel() {

    val books: StateFlow<List<Book>> = getBooksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
