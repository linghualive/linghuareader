package com.linghualive.flamekit.core.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Bookshelf : Screen

    @Serializable
    data class Reader(val bookId: Long) : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object SourceManager : Screen

    @Serializable
    data class Search(val keyword: String = "") : Screen

    @Serializable
    data class OnlineBookDetail(val sourceUrl: String, val bookUrl: String) : Screen

    @Serializable
    data object Stats : Screen

    @Serializable
    data object Sync : Screen

    @Serializable
    data class SourceEditor(val sourceUrl: String? = null) : Screen
}
