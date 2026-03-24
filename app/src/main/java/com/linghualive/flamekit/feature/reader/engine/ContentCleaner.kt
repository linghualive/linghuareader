package com.linghualive.flamekit.feature.reader.engine

import javax.inject.Inject

class ContentCleaner @Inject constructor() {
    private val defaultRules = listOf(
        Regex("(?i)<br\\s*/?>"),
        Regex("&nbsp;"),
        Regex("\\s*手机用户请浏览.*?阅读.*"),
        Regex("\\s*天才一秒记住.*"),
        Regex("\\s*最新章节.*?请收藏.*"),
        Regex("\\s*本章未完.*?点击下一页.*"),
        Regex("\\s*https?://\\S+"),
        Regex("\\s*请收藏本站.*"),
        Regex("\\s*笔趣阁.*?最新章节.*"),
        Regex("\\s*一秒记住.*?免费阅读.*"),
    )

    fun clean(content: String, customRules: List<String> = emptyList()): String {
        var cleaned = content
        for (rule in defaultRules) {
            cleaned = cleaned.replace(rule, "")
        }
        for (ruleStr in customRules) {
            try {
                cleaned = cleaned.replace(Regex(ruleStr), "")
            } catch (_: Exception) {
                // Skip invalid regex patterns
            }
        }
        return cleaned.trim()
    }
}
