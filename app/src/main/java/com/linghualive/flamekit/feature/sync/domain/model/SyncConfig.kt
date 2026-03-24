package com.linghualive.flamekit.feature.sync.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncConfig(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val remotePath: String = "/FlameKit/",
    val autoSync: Boolean = false,
    val lastSyncTime: Long = 0,
)
