package com.linghualive.flamekit.feature.bookshelf.domain.usecase

import android.net.Uri
import com.linghualive.flamekit.feature.bookshelf.domain.model.Book
import com.linghualive.flamekit.feature.bookshelf.domain.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class DeleteBookUseCase @Inject constructor(
    private val repository: BookRepository,
) {

    suspend operator fun invoke(book: Book) = withContext(Dispatchers.IO) {
        val uri = Uri.parse(book.filePath)
        val path = uri.path
        if (path != null) {
            File(path).delete()
        }
        repository.deleteBook(book)
    }
}
