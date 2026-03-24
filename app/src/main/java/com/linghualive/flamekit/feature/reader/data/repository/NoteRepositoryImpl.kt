package com.linghualive.flamekit.feature.reader.data.repository

import com.linghualive.flamekit.core.database.dao.NoteDao
import com.linghualive.flamekit.core.database.entity.NoteEntity
import com.linghualive.flamekit.feature.reader.data.mapper.toDomain
import com.linghualive.flamekit.feature.reader.domain.model.Note
import com.linghualive.flamekit.feature.reader.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
) : NoteRepository {

    override fun getNotesByBook(bookId: Long): Flow<List<Note>> {
        return noteDao.getNotesByBook(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getNotesByChapter(bookId: Long, chapterIndex: Int): Flow<List<Note>> {
        return noteDao.getNotesByChapter(bookId, chapterIndex).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addNote(
        bookId: Long,
        chapterIndex: Int,
        startPos: Int,
        endPos: Int,
        selectedText: String,
        noteContent: String?,
        color: Long,
    ): Long {
        return noteDao.insert(
            NoteEntity(
                bookId = bookId,
                chapterIndex = chapterIndex,
                startPosition = startPos,
                endPosition = endPos,
                selectedText = selectedText,
                noteContent = noteContent,
                highlightColor = color,
            )
        )
    }

    override suspend fun updateNote(noteId: Long, content: String) {
        val existing = noteDao.getNoteById(noteId) ?: return
        noteDao.update(existing.copy(noteContent = content, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteNote(noteId: Long) {
        noteDao.deleteById(noteId)
    }
}
