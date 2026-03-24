package com.linghualive.flamekit.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_sources")
data class BookSourceEntity(
    @PrimaryKey
    val sourceUrl: String,
    val sourceName: String,
    val sourceGroup: String? = null,
    val sourceType: Int = 0,
    val enabled: Boolean = true,
    val header: String? = null,
    val loginUrl: String? = null,
    val lastUpdateTime: Long = 0,

    val searchUrl: String? = null,
    val searchList: String? = null,
    val searchName: String? = null,
    val searchAuthor: String? = null,
    val searchCover: String? = null,
    val searchBookUrl: String? = null,

    val detailName: String? = null,
    val detailAuthor: String? = null,
    val detailCover: String? = null,
    val detailIntro: String? = null,
    val detailTocUrl: String? = null,

    val tocList: String? = null,
    val tocName: String? = null,
    val tocUrl: String? = null,

    val contentRule: String? = null,
    val contentNextUrl: String? = null,

    val lastCheckTime: Long = 0,
    val lastCheckSuccess: Boolean = true,
)
