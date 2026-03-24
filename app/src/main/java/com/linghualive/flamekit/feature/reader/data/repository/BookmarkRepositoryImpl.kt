package com.linghualive.flamekit.feature.reader.data.repository

import com.linghualive.flamekit.core.database.dao.BookmarkDao
import com.linghualive.flamekit.feature.reader.data.mapper.toDomain
import com.linghualive.flamekit.feature.reader.data.mapper.toEntity
import com.linghualive.flamekit.feature.reader.domain.model.Bookmark
import com.linghualive.flamekit.feature.reader.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao,
) : BookmarkRepository {

    override fun getBookmarks(bookId: Long): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksByBookId(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addBookmark(bookmark: Bookmark): Long {
        return bookmarkDao.insertBookmark(bookmark.toEntity())
    }

    override suspend fun deleteBookmark(bookmark: Bookmark) {
        bookmarkDao.deleteBookmark(bookmark.toEntity())
    }
}
