package com.linghualive.flamekit.feature.bookshelf.domain.usecase

import com.linghualive.flamekit.feature.bookshelf.domain.model.Book
import com.linghualive.flamekit.feature.bookshelf.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBooksUseCase @Inject constructor(
    private val repository: BookRepository,
) {
    operator fun invoke(): Flow<List<Book>> = repository.getAllBooks()
}
