package com.linghualive.flamekit.feature.reader.data.mapper

import com.linghualive.flamekit.core.database.entity.NoteEntity
import com.linghualive.flamekit.feature.reader.domain.model.Note

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    bookId = bookId,
    chapterIndex = chapterIndex,
    startPosition = startPosition,
    endPosition = endPosition,
    selectedText = selectedText,
    noteContent = noteContent,
    highlightColor = highlightColor,
    createdAt = createdAt,
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    bookId = bookId,
    chapterIndex = chapterIndex,
    startPosition = startPosition,
    endPosition = endPosition,
    selectedText = selectedText,
    noteContent = noteContent,
    highlightColor = highlightColor,
    createdAt = createdAt,
)
