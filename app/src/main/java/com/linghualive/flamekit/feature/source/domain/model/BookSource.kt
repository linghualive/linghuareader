package com.linghualive.flamekit.feature.source.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BookSource(
    val sourceUrl: String,
    val sourceName: String,
    val sourceGroup: String? = null,
    val sourceType: Int = 0,
    val enabled: Boolean = true,
    val header: String? = null,
    val loginUrl: String? = null,
    val lastUpdateTime: Long = 0,

    // Search rules
    val searchUrl: String? = null,
    val searchList: String? = null,
    val searchName: String? = null,
    val searchAuthor: String? = null,
    val searchCover: String? = null,
    val searchBookUrl: String? = null,

    // Detail rules
    val detailName: String? = null,
    val detailAuthor: String? = null,
    val detailCover: String? = null,
    val detailIntro: String? = null,
    val detailTocUrl: String? = null,

    // TOC rules
    val tocList: String? = null,
    val tocName: String? = null,
    val tocUrl: String? = null,

    // Content rules
    val contentRule: String? = null,
    val contentNextUrl: String? = null,

    // Health check
    val lastCheckTime: Long = 0,
    val lastCheckSuccess: Boolean = true,
)
