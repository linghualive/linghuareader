package com.linghualive.flamekit.feature.reader.domain.repository

import com.linghualive.flamekit.feature.reader.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotesByBook(bookId: Long): Flow<List<Note>>
    fun getNotesByChapter(bookId: Long, chapterIndex: Int): Flow<List<Note>>
    suspend fun addNote(
        bookId: Long,
        chapterIndex: Int,
        startPos: Int,
        endPos: Int,
        selectedText: String,
        noteContent: String?,
        color: Long,
    ): Long
    suspend fun updateNote(noteId: Long, content: String)
    suspend fun deleteNote(noteId: Long)
}
