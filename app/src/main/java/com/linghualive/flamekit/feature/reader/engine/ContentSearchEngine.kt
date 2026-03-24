package com.linghualive.flamekit.feature.reader.engine

import com.linghualive.flamekit.feature.reader.domain.model.SearchResult
import com.linghualive.flamekit.feature.reader.domain.repository.ReaderRepository
import javax.inject.Inject

class ContentSearchEngine @Inject constructor(
    private val readerRepository: ReaderRepository,
) {
    suspend fun search(bookId: Long, keyword: String): List<SearchResult> {
        if (keyword.isBlank()) return emptyList()

        val chapters = readerRepository.loadChapters(bookId)
        val results = mutableListOf<SearchResult>()

        for (chapter in chapters) {
            val content = readerRepository.loadChapterContent(bookId, chapter.index)
            val lowerContent = content.lowercase()
            val lowerKeyword = keyword.lowercase()

            var searchFrom = 0
            while (true) {
                val pos = lowerContent.indexOf(lowerKeyword, searchFrom)
                if (pos == -1) break

                val contextStart = (pos - 20).coerceAtLeast(0)
                val contextEnd = (pos + keyword.length + 20).coerceAtMost(content.length)
                val matchText = content.substring(contextStart, contextEnd)

                results.add(
                    SearchResult(
                        chapterIndex = chapter.index,
                        chapterTitle = chapter.title,
                        matchText = matchText,
                        matchPosition = pos,
                        keyword = keyword,
                    )
                )

                searchFrom = pos + keyword.length
            }
        }

        return results
    }
}
