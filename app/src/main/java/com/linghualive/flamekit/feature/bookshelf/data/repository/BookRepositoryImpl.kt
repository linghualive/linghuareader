package com.linghualive.flamekit.feature.bookshelf.data.repository

import com.linghualive.flamekit.core.database.dao.BookDao
import com.linghualive.flamekit.feature.bookshelf.data.mapper.toDomain
import com.linghualive.flamekit.feature.bookshelf.data.mapper.toEntity
import com.linghualive.flamekit.feature.bookshelf.domain.model.Book
import com.linghualive.flamekit.feature.bookshelf.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
) : BookRepository {

    override fun getAllBooks(): Flow<List<Book>> =
        bookDao.getAllBooks().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addBook(book: Book): Long =
        bookDao.insertBook(book.toEntity())

    override suspend fun deleteBook(book: Book) =
        bookDao.deleteBook(book.toEntity())

    override suspend fun updateLastRead(bookId: Long) =
        bookDao.updateLastReadAt(bookId)
}
