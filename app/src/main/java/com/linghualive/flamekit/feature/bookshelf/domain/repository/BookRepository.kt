package com.linghualive.flamekit.feature.bookshelf.domain.repository

import com.linghualive.flamekit.feature.bookshelf.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(): Flow<List<Book>>
    suspend fun addBook(book: Book): Long
    suspend fun deleteBook(book: Book)
    suspend fun updateLastRead(bookId: Long)
}
