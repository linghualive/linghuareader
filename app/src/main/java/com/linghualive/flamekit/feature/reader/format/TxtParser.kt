package com.linghualive.flamekit.feature.reader.format

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TxtParser @Inject constructor(
    private val encodingDetector: EncodingDetector,
) : BookParser {

    private val chapterPatterns = listOf(
        Regex("^\\s*第[零一二三四五六七八九十百千万\\d]+[章节回集卷].*$", RegexOption.MULTILINE),
        Regex("^\\s*Chapter\\s+\\d+.*$", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)),
        Regex("^\\s*(?:楔子|序章|序言|引子|尾声|番外|后记|前言).*$", RegexOption.MULTILINE),
    )

    companion object {
        private const val FALLBACK_CHAPTER_SIZE = 5000
    }

    override suspend fun parse(context: Context, uri: Uri): ParseResult {
        val chapters = parseChapters(context, uri)
        return ParseResult(
            title = "",
            author = null,
            chapters = chapters,
        )
    }

    suspend fun parseChapters(context: Context, uri: Uri): List<Chapter> = withContext(Dispatchers.IO) {
        val encoding = context.contentResolver.openInputStream(uri)?.use { stream ->
            encodingDetector.detect(stream)
        } ?: "UTF-8"

        val fullText = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(charset(encoding)).readText()
        } ?: return@withContext emptyList()

        val matches = mutableListOf<Pair<Int, String>>()
        for (pattern in chapterPatterns) {
            for (result in pattern.findAll(fullText)) {
                matches.add(result.range.first to result.value.trim())
            }
        }
        matches.sortBy { it.first }

        if (matches.isEmpty()) {
            return@withContext splitBySize(fullText)
        }

        buildChapters(fullText, matches)
    }

    private fun buildChapters(
        fullText: String,
        matches: List<Pair<Int, String>>,
    ): List<Chapter> {
        val chapters = mutableListOf<Chapter>()

        if (matches.first().first > 0) {
            val preContent = fullText.substring(0, matches.first().first).trim()
            if (preContent.isNotEmpty()) {
                chapters.add(
                    Chapter(
                        index = 0,
                        title = "前言",
                        content = preContent,
                        startOffset = 0L,
                    )
                )
            }
        }

        for (i in matches.indices) {
            val start = matches[i].first
            val end = if (i + 1 < matches.size) matches[i + 1].first else fullText.length
            val content = fullText.substring(start, end).trim()

            chapters.add(
                Chapter(
                    index = chapters.size,
                    title = matches[i].second,
                    content = content,
                    startOffset = start.toLong(),
                )
            )
        }

        return chapters
    }

    private fun splitBySize(fullText: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        var offset = 0
        var index = 0

        while (offset < fullText.length) {
            val end = (offset + FALLBACK_CHAPTER_SIZE).coerceAtMost(fullText.length)
            val content = fullText.substring(offset, end).trim()
            if (content.isNotEmpty()) {
                chapters.add(
                    Chapter(
                        index = index,
                        title = "第${index + 1}段",
                        content = content,
                        startOffset = offset.toLong(),
                    )
                )
                index++
            }
            offset = end
        }

        return chapters
    }
}
