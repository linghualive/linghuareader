package com.linghualive.flamekit.feature.source.engine

import com.linghualive.flamekit.feature.source.domain.model.BookDetail
import com.linghualive.flamekit.feature.source.domain.model.BookSource
import com.linghualive.flamekit.feature.source.domain.model.OnlineChapter
import com.linghualive.flamekit.feature.source.domain.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI
import javax.inject.Inject

class SourceExecutor @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val ruleParser: RuleParser,
) {

    suspend fun search(source: BookSource, keyword: String): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val searchUrl = source.searchUrl ?: return@withContext emptyList()
            val url = searchUrl.replace("{{key}}", java.net.URLEncoder.encode(keyword, "UTF-8"))
            val html = fetchHtml(url, parseHeaders(source.header))

            val listRule = source.searchList ?: return@withContext emptyList()
            val elements = ruleParser.parseElements(html, listRule)

            elements.mapNotNull { element ->
                val bookName = source.searchName?.let { ruleParser.extractFromElement(element, it) }
                    ?: return@mapNotNull null
                val bookUrl = source.searchBookUrl?.let { ruleParser.extractFromElement(element, it) }
                    ?: return@mapNotNull null

                SearchResult(
                    bookName = bookName,
                    author = source.searchAuthor?.let { ruleParser.extractFromElement(element, it) },
                    coverUrl = source.searchCover?.let {
                        ruleParser.extractFromElement(element, it)?.let { cover ->
                            resolveUrl(source.sourceUrl, cover)
                        }
                    },
                    bookUrl = resolveUrl(source.sourceUrl, bookUrl),
                    sourceName = source.sourceName,
                    sourceUrl = source.sourceUrl,
                )
            }
        }

    suspend fun getDetail(source: BookSource, bookUrl: String): BookDetail =
        withContext(Dispatchers.IO) {
            val html = fetchHtml(bookUrl, parseHeaders(source.header))

            val name = source.detailName?.let { ruleParser.extractFromHtml(html, it) } ?: ""
            val author = source.detailAuthor?.let { ruleParser.extractFromHtml(html, it) }
            val cover = source.detailCover?.let {
                ruleParser.extractFromHtml(html, it)?.let { c -> resolveUrl(source.sourceUrl, c) }
            }
            val intro = source.detailIntro?.let { ruleParser.extractFromHtml(html, it) }
            val tocUrl = source.detailTocUrl?.let {
                ruleParser.extractFromHtml(html, it)?.let { t -> resolveUrl(source.sourceUrl, t) }
            }

            val chapters = if (tocUrl != null) {
                getChapters(source, tocUrl)
            } else {
                parseChaptersFromHtml(source, html)
            }

            BookDetail(
                name = name,
                author = author,
                coverUrl = cover,
                intro = intro,
                tocUrl = tocUrl,
                chapters = chapters,
            )
        }

    suspend fun getChapters(source: BookSource, tocUrl: String): List<OnlineChapter> =
        withContext(Dispatchers.IO) {
            val html = fetchHtml(tocUrl, parseHeaders(source.header))
            parseChaptersFromHtml(source, html)
        }

    private fun parseChaptersFromHtml(source: BookSource, html: String): List<OnlineChapter> {
        val listRule = source.tocList ?: return emptyList()
        val elements = ruleParser.parseElements(html, listRule)

        return elements.mapNotNull { element ->
            val title = source.tocName?.let { ruleParser.extractFromElement(element, it) }
                ?: return@mapNotNull null
            val url = source.tocUrl?.let { ruleParser.extractFromElement(element, it) }
                ?: return@mapNotNull null

            OnlineChapter(
                title = title,
                url = resolveUrl(source.sourceUrl, url),
            )
        }
    }

    suspend fun getContent(source: BookSource, chapterUrl: String): String =
        withContext(Dispatchers.IO) {
            val html = fetchHtml(chapterUrl, parseHeaders(source.header))
            val rule = source.contentRule ?: return@withContext ""

            val content = ruleParser.extractFromHtml(html, rule) ?: ""

            // Handle paginated content
            val nextUrl = source.contentNextUrl?.let {
                ruleParser.extractFromHtml(html, it)
            }?.let { resolveUrl(source.sourceUrl, it) }

            if (nextUrl != null && nextUrl != chapterUrl) {
                val nextContent = getContent(source, nextUrl)
                "$content\n$nextContent"
            } else {
                formatContent(content)
            }
        }

    private fun formatContent(rawContent: String): String {
        // Clean up HTML content to plain text
        val doc = Jsoup.parse(rawContent)
        doc.select("br").append("\n")
        doc.select("p").prepend("\n")
        return doc.text()
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private suspend fun fetchHtml(url: String, headers: Map<String, String>? = null): String =
        withContext(Dispatchers.IO) {
            val requestBuilder = Request.Builder().url(url)
            headers?.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }
            requestBuilder.addHeader(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            response.body?.string() ?: ""
        }

    private fun parseHeaders(headerJson: String?): Map<String, String>? {
        if (headerJson.isNullOrBlank()) return null
        return try {
            val jsonObject = Json.decodeFromString<JsonObject>(headerJson)
            jsonObject.mapValues { it.value.jsonPrimitive.content }
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveUrl(baseUrl: String, url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        return try {
            URI(baseUrl).resolve(url).toString()
        } catch (e: Exception) {
            url
        }
    }
}
