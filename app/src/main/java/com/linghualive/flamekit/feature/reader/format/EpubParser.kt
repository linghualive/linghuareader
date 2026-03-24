package com.linghualive.flamekit.feature.reader.format

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.w3c.dom.Element
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

class EpubParser @Inject constructor() : BookParser {

    override suspend fun parse(context: Context, uri: Uri): ParseResult =
        withContext(Dispatchers.IO) {
            val entries = mutableMapOf<String, ByteArray>()

            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            entries[entry.name] = zip.readBytes()
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return@withContext ParseResult(
                title = "Unknown",
                author = null,
                chapters = emptyList(),
            )

            val rootFilePath = findRootFilePath(entries)
                ?: return@withContext ParseResult(
                    title = "Unknown",
                    author = null,
                    chapters = emptyList(),
                )

            val opfContent = entries[rootFilePath]
                ?: return@withContext ParseResult(
                    title = "Unknown",
                    author = null,
                    chapters = emptyList(),
                )

            val opfBaseDir = rootFilePath.substringBeforeLast("/", "")
            val opfDoc = parseXml(opfContent)

            val title = extractMetadata(opfDoc, "title") ?: "Unknown"
            val author = extractMetadata(opfDoc, "creator")

            val manifest = parseManifest(opfDoc, opfBaseDir)
            val spineItemIds = parseSpine(opfDoc)
            val toc = parseToc(entries, manifest, opfDoc, opfBaseDir)

            val chapters = mutableListOf<Chapter>()
            var chapterIndex = 0

            for (itemId in spineItemIds) {
                val href = manifest[itemId] ?: continue
                val data = entries[href] ?: continue
                val html = String(data, Charsets.UTF_8)
                val doc = Jsoup.parse(html)

                val paragraphs = doc.body().select("p, h1, h2, h3, h4, h5, h6, div")
                val textContent = if (paragraphs.isNotEmpty()) {
                    paragraphs.joinToString("\n\n") { it.text() }
                } else {
                    doc.body().text()
                }

                if (textContent.isBlank()) continue

                val chapterTitle = toc[href]
                    ?: doc.select("h1, h2, h3").firstOrNull()?.text()
                    ?: "Chapter ${chapterIndex + 1}"

                chapters.add(
                    Chapter(
                        index = chapterIndex,
                        title = chapterTitle,
                        content = textContent.trim(),
                        startOffset = 0L,
                    )
                )
                chapterIndex++
            }

            val coverPath = extractCover(opfDoc, manifest, entries, context)

            ParseResult(
                title = title,
                author = author,
                chapters = chapters,
                coverPath = coverPath,
            )
        }

    private fun findRootFilePath(entries: Map<String, ByteArray>): String? {
        val containerXml = entries["META-INF/container.xml"] ?: return null
        val doc = parseXml(containerXml)
        val rootFiles = doc.getElementsByTagName("rootfile")
        if (rootFiles.length > 0) {
            return (rootFiles.item(0) as Element).getAttribute("full-path")
        }
        return null
    }

    private fun parseXml(data: ByteArray): org.w3c.dom.Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        return factory.newDocumentBuilder().parse(data.inputStream())
    }

    private fun extractMetadata(doc: org.w3c.dom.Document, localName: String): String? {
        val nodes = doc.getElementsByTagName("dc:$localName")
        if (nodes.length > 0) {
            return nodes.item(0).textContent?.takeIf { it.isNotBlank() }
        }
        val nodesByLocal = doc.getElementsByTagNameNS("http://purl.org/dc/elements/1.1/", localName)
        if (nodesByLocal.length > 0) {
            return nodesByLocal.item(0).textContent?.takeIf { it.isNotBlank() }
        }
        return null
    }

    private fun parseManifest(
        doc: org.w3c.dom.Document,
        opfBaseDir: String,
    ): Map<String, String> {
        val manifest = mutableMapOf<String, String>()
        val items = doc.getElementsByTagName("item")
        for (i in 0 until items.length) {
            val item = items.item(i) as Element
            val id = item.getAttribute("id")
            val href = item.getAttribute("href")
            val fullPath = if (opfBaseDir.isNotEmpty()) "$opfBaseDir/$href" else href
            manifest[id] = fullPath
        }
        return manifest
    }

    private fun parseSpine(doc: org.w3c.dom.Document): List<String> {
        val itemRefs = doc.getElementsByTagName("itemref")
        return (0 until itemRefs.length).map { i ->
            (itemRefs.item(i) as Element).getAttribute("idref")
        }
    }

    private fun parseToc(
        entries: Map<String, ByteArray>,
        manifest: Map<String, String>,
        opfDoc: org.w3c.dom.Document,
        opfBaseDir: String,
    ): Map<String, String> {
        val toc = mutableMapOf<String, String>()

        // Try NCX TOC (EPUB 2)
        val spineElement = opfDoc.getElementsByTagName("spine")
        if (spineElement.length > 0) {
            val tocId = (spineElement.item(0) as Element).getAttribute("toc")
            if (tocId.isNotBlank()) {
                val ncxPath = manifest[tocId]
                val ncxData = ncxPath?.let { entries[it] }
                if (ncxData != null) {
                    val ncxDoc = parseXml(ncxData)
                    val navPoints = ncxDoc.getElementsByTagName("navPoint")
                    for (i in 0 until navPoints.length) {
                        val navPoint = navPoints.item(i) as Element
                        val label = navPoint.getElementsByTagName("text")
                            .let { if (it.length > 0) it.item(0).textContent else null }
                        val contentSrc = navPoint.getElementsByTagName("content")
                            .let {
                                if (it.length > 0) (it.item(0) as Element).getAttribute("src")
                                else null
                            }
                        if (label != null && contentSrc != null) {
                            val fullSrc = if (opfBaseDir.isNotEmpty()) {
                                "$opfBaseDir/${contentSrc.substringBefore("#")}"
                            } else {
                                contentSrc.substringBefore("#")
                            }
                            toc[fullSrc] = label
                        }
                    }
                }
            }
        }

        // Try nav TOC (EPUB 3) if NCX didn't yield results
        if (toc.isEmpty()) {
            val navPath = manifest.values.find { path ->
                val data = entries[path]
                data != null && String(data, Charsets.UTF_8).contains("nav", ignoreCase = true)
                        && path.endsWith(".xhtml", ignoreCase = true)
            }
            if (navPath != null) {
                val navData = entries[navPath]
                if (navData != null) {
                    val navDoc = Jsoup.parse(String(navData, Charsets.UTF_8))
                    val navElement = navDoc.select("nav[*|type=toc], nav#toc").first()
                    navElement?.select("a[href]")?.forEach { a ->
                        val href = a.attr("href").substringBefore("#")
                        val navBaseDir = navPath.substringBeforeLast("/", "")
                        val fullHref = if (navBaseDir.isNotEmpty()) "$navBaseDir/$href" else href
                        toc[fullHref] = a.text()
                    }
                }
            }
        }

        return toc
    }

    private fun extractCover(
        opfDoc: org.w3c.dom.Document,
        manifest: Map<String, String>,
        entries: Map<String, ByteArray>,
        context: Context,
    ): String? {
        // Look for cover in metadata
        val metaNodes = opfDoc.getElementsByTagName("meta")
        var coverId: String? = null
        for (i in 0 until metaNodes.length) {
            val meta = metaNodes.item(i) as Element
            if (meta.getAttribute("name") == "cover") {
                coverId = meta.getAttribute("content")
                break
            }
        }

        // Look for item with properties="cover-image" (EPUB 3)
        if (coverId == null) {
            val items = opfDoc.getElementsByTagName("item")
            for (i in 0 until items.length) {
                val item = items.item(i) as Element
                if (item.getAttribute("properties").contains("cover-image")) {
                    coverId = item.getAttribute("id")
                    break
                }
            }
        }

        if (coverId == null) return null

        val coverPath = manifest[coverId] ?: return null
        val coverData = entries[coverPath] ?: return null

        val ext = coverPath.substringAfterLast(".", "jpg")
        val coverFile = File(context.filesDir, "covers/${System.currentTimeMillis()}.$ext")
        coverFile.parentFile?.mkdirs()
        coverFile.writeBytes(coverData)

        return coverFile.absolutePath
    }
}
