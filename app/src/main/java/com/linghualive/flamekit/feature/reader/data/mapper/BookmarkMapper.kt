package com.linghualive.flamekit.feature.reader.data.mapper

import com.linghualive.flamekit.core.database.entity.BookmarkEntity
import com.linghualive.flamekit.feature.reader.domain.model.Bookmark

fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    bookId = bookId,
    chapterIndex = chapterIndex,
    position = position,
    title = title,
    createdAt = createdAt,
)

fun Bookmark.toEntity(): BookmarkEntity = BookmarkEntity(
    id = id,
    bookId = bookId,
    chapterIndex = chapterIndex,
    position = position,
    title = title,
    createdAt = createdAt,
)
