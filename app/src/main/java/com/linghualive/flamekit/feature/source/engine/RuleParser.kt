package com.linghualive.flamekit.feature.source.engine

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import javax.inject.Inject

class RuleParser @Inject constructor() {

    fun parseElements(html: String, listRule: String): List<Element> {
        val doc = Jsoup.parse(html)
        val cssSelector = listRule.split("@").first().trim()
        return doc.select(cssSelector).toList()
    }

    fun extractFromElement(element: Element, rule: String): String? {
        if (rule.isBlank()) return null

        // Handle multi-rule with || fallback
        if ("||" in rule) {
            for (subRule in rule.split("||")) {
                val result = extractFromElement(element, subRule.trim())
                if (!result.isNullOrBlank()) return result
            }
            return null
        }

        // Handle regex rule
        if (rule.startsWith("regex::")) {
            val pattern = rule.removePrefix("regex::")
            val text = element.text()
            val match = Regex(pattern).find(text)
            return match?.groupValues?.getOrNull(1) ?: match?.value
        }

        // Handle CSS selector with attribute extraction: "selector@attr"
        val parts = rule.split("@", limit = 2)
        val selector = parts[0].trim()
        val attr = parts.getOrNull(1)?.trim()

        val target = if (selector.isEmpty()) element else {
            element.selectFirst(selector) ?: return null
        }

        return when (attr) {
            null, "text" -> target.text().takeIf { it.isNotBlank() }
            "textNodes" -> target.textNodes().joinToString("\n") { it.text().trim() }
                .takeIf { it.isNotBlank() }
            "html" -> target.html().takeIf { it.isNotBlank() }
            "href" -> target.attr("abs:href").takeIf { it.isNotBlank() }
                ?: target.attr("href").takeIf { it.isNotBlank() }
            "src" -> target.attr("abs:src").takeIf { it.isNotBlank() }
                ?: target.attr("src").takeIf { it.isNotBlank() }
            else -> target.attr(attr).takeIf { it.isNotBlank() }
        }
    }

    fun extractFromHtml(html: String, rule: String): String? {
        if (rule.isBlank()) return null
        val doc = Jsoup.parse(html)
        return extractFromElement(doc, rule)
    }

    fun extractAllFromHtml(html: String, listRule: String, itemRule: String): List<String> {
        val elements = parseElements(html, listRule)
        return elements.mapNotNull { extractFromElement(it, itemRule) }
    }
}
