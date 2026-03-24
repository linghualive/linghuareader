package com.linghualive.flamekit.feature.bookshelf.data.mapper

import com.linghualive.flamekit.core.database.entity.BookEntity
import com.linghualive.flamekit.feature.bookshelf.domain.model.Book

fun BookEntity.toDomain(): Book = Book(
    id = id,
    title = title,
    author = author,
    filePath = filePath,
    format = format,
    coverPath = coverPath,
    addedAt = addedAt,
    lastReadAt = lastReadAt,
    totalChapters = totalChapters,
    sourceUrl = sourceUrl,
)

fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    title = title,
    author = author,
    filePath = filePath,
    format = format,
    coverPath = coverPath,
    addedAt = addedAt,
    lastReadAt = lastReadAt,
    totalChapters = totalChapters,
    sourceUrl = sourceUrl,
)
