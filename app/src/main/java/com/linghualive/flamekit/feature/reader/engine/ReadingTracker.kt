package com.linghualive.flamekit.feature.reader.engine

import com.linghualive.flamekit.core.database.dao.ReadingStatDao
import com.linghualive.flamekit.core.database.entity.ReadingStatEntity
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingTracker @Inject constructor(
    private val readingStatDao: ReadingStatDao,
) {
    private var startTime: Long = 0
    private var currentBookId: Long = 0
    private var pagesRead: Int = 0
    private var chaptersRead: Int = 0
    private var isPaused: Boolean = false
    private var accumulatedSeconds: Long = 0

    fun startTracking(bookId: Long) {
        currentBookId = bookId
        startTime = System.currentTimeMillis()
        pagesRead = 0
        chaptersRead = 0
        isPaused = false
        accumulatedSeconds = 0
    }

    fun onPageTurned() {
        pagesRead++
    }

    fun onChapterChanged() {
        chaptersRead++
    }

    fun pause() {
        if (!isPaused && startTime > 0) {
            accumulatedSeconds += (System.currentTimeMillis() - startTime) / 1000
            isPaused = true
        }
    }

    fun resume() {
        if (isPaused) {
            startTime = System.currentTimeMillis()
            isPaused = false
        }
    }

    suspend fun stopTracking() {
        if (startTime == 0L && accumulatedSeconds == 0L) return

        val duration = accumulatedSeconds + if (!isPaused && startTime > 0) {
            (System.currentTimeMillis() - startTime) / 1000
        } else {
            0
        }

        if (duration <= 0) {
            reset()
            return
        }

        val today = LocalDate.now().toString()
        val existing = readingStatDao.getStatsByDate(today)
            .firstOrNull { it.bookId == currentBookId }

        readingStatDao.upsert(
            ReadingStatEntity(
                id = existing?.id ?: 0,
                bookId = currentBookId,
                date = today,
                durationSeconds = (existing?.durationSeconds ?: 0) + duration,
                pagesRead = (existing?.pagesRead ?: 0) + pagesRead,
                chaptersRead = (existing?.chaptersRead ?: 0) + chaptersRead,
            )
        )
        reset()
    }

    private fun reset() {
        startTime = 0
        accumulatedSeconds = 0
        pagesRead = 0
        chaptersRead = 0
        isPaused = false
    }
}
