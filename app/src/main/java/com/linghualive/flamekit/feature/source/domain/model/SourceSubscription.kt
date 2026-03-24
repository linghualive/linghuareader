package com.linghualive.flamekit.feature.source.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SourceSubscription(
    val name: String,
    val url: String,
    val lastUpdate: Long = 0,
    val enabled: Boolean = true,
    val sourceCount: Int = 0,
)
