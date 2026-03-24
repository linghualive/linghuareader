package com.linghualive.flamekit.feature.source.engine

import org.jsoup.Jsoup
import javax.inject.Inject

class RuleEngine @Inject constructor() {

    fun evaluate(rule: String, content: String): List<String> {
        if (rule.isBlank() || content.isBlank()) return emptyList()

        // Handle multi-rule with || fallback
        if ("||" in rule) {
            for (subRule in rule.split("||")) {
                val result = evaluate(subRule.trim(), content)
                if (result.isNotEmpty()) return result
            }
            return emptyList()
        }

        // Handle regex rule
        if (rule.startsWith("regex::")) {
            val pattern = rule.removePrefix("regex::")
            return Regex(pattern).findAll(content)
                .mapNotNull { it.groupValues.getOrNull(1) ?: it.value }
                .toList()
        }

        // CSS selector with attribute: "selector@attr"
        val parts = rule.split("@", limit = 2)
        val selector = parts[0].trim()
        val attr = parts.getOrNull(1)?.trim()

        val doc = Jsoup.parse(content)
        val elements = if (selector.isEmpty()) listOf(doc) else doc.select(selector)

        return elements.mapNotNull { element ->
            when (attr) {
                null, "text" -> element.text().takeIf { it.isNotBlank() }
                "textNodes" -> element.textNodes().joinToString("\n") { it.text().trim() }
                    .takeIf { it.isNotBlank() }
                "html" -> element.html().takeIf { it.isNotBlank() }
                "href" -> element.attr("abs:href").takeIf { it.isNotBlank() }
                    ?: element.attr("href").takeIf { it.isNotBlank() }
                "src" -> element.attr("abs:src").takeIf { it.isNotBlank() }
                    ?: element.attr("src").takeIf { it.isNotBlank() }
                else -> element.attr(attr).takeIf { it.isNotBlank() }
            }
        }
    }

    fun evaluateFirst(rule: String, content: String): String? {
        return evaluate(rule, content).firstOrNull()
    }
}
