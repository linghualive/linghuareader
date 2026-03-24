package com.linghualive.flamekit.feature.update.domain.model

data class AppRelease(
    val version: String,
    val name: String,
    val description: String,
    val downloadUrl: String,
    val htmlUrl: String,
    val hasDirectDownload: Boolean,
)
