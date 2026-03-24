package com.linghualive.flamekit.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.linghualive.flamekit.feature.sync.domain.model.SyncConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingPrefsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val key = stringPreferencesKey("reading_preferences")
    private val syncConfigKey = stringPreferencesKey("sync_config")
    private val json = Json { ignoreUnknownKeys = true }

    val preferencesFlow: Flow<ReadingPreferences> = dataStore.data.map { prefs ->
        val raw = prefs[key]
        if (raw != null) {
            try {
                json.decodeFromString<ReadingPreferences>(raw)
            } catch (_: Exception) {
                ReadingPreferences()
            }
        } else {
            ReadingPreferences()
        }
    }

    suspend fun getPreferences(): ReadingPreferences {
        return preferencesFlow.first()
    }

    suspend fun update(transform: (ReadingPreferences) -> ReadingPreferences) {
        dataStore.edit { prefs ->
            val current = prefs[key]?.let {
                try { json.decodeFromString<ReadingPreferences>(it) } catch (_: Exception) { null }
            } ?: ReadingPreferences()
            val updated = transform(current)
            prefs[key] = json.encodeToString(updated)
        }
    }

    val syncConfigFlow: Flow<SyncConfig> = dataStore.data.map { prefs ->
        val raw = prefs[syncConfigKey]
        if (raw != null) {
            try {
                json.decodeFromString<SyncConfig>(raw)
            } catch (_: Exception) {
                SyncConfig()
            }
        } else {
            SyncConfig()
        }
    }

    suspend fun getSyncConfig(): SyncConfig {
        return syncConfigFlow.first()
    }

    suspend fun updateSyncConfig(transform: (SyncConfig) -> SyncConfig) {
        dataStore.edit { prefs ->
            val current = prefs[syncConfigKey]?.let {
                try { json.decodeFromString<SyncConfig>(it) } catch (_: Exception) { null }
            } ?: SyncConfig()
            val updated = transform(current)
            prefs[syncConfigKey] = json.encodeToString(updated)
        }
    }
}
