package com.linghualive.flamekit.feature.sync.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linghualive.flamekit.core.datastore.ReadingPrefsDataStore
import com.linghualive.flamekit.feature.sync.data.BackupManager
import com.linghualive.flamekit.feature.sync.data.WebDavClient
import com.linghualive.flamekit.feature.sync.domain.model.SyncConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
}

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val webDavClient: WebDavClient,
    private val readingPrefsDataStore: ReadingPrefsDataStore,
) : ViewModel() {

    val syncConfig: StateFlow<SyncConfig> = readingPrefsDataStore.syncConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncConfig())

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun updateConfig(config: SyncConfig) {
        viewModelScope.launch {
            readingPrefsDataStore.updateSyncConfig { config }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _syncState.value = SyncState.SYNCING
            val config = syncConfig.value
            val result = webDavClient.exists(
                config.serverUrl, config.username, config.password, config.remotePath,
            )
            result.onSuccess {
                _syncState.value = SyncState.SUCCESS
                _message.value = "连接成功"
            }.onFailure { e ->
                _syncState.value = SyncState.ERROR
                _message.value = "连接失败: ${e.message}"
            }
        }
    }

    fun backup() {
        viewModelScope.launch {
            _syncState.value = SyncState.SYNCING
            val config = syncConfig.value
            backupManager.backup(config)
                .onSuccess {
                    _syncState.value = SyncState.SUCCESS
                    _message.value = "备份成功"
                }
                .onFailure { e ->
                    _syncState.value = SyncState.ERROR
                    _message.value = "备份失败: ${e.message}"
                }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _syncState.value = SyncState.SYNCING
            val config = syncConfig.value
            backupManager.restore(config)
                .onSuccess {
                    _syncState.value = SyncState.SUCCESS
                    _message.value = "恢复成功"
                }
                .onFailure { e ->
                    _syncState.value = SyncState.ERROR
                    _message.value = "恢复失败: ${e.message}"
                }
        }
    }

    fun importLocal(uri: Uri) {
        viewModelScope.launch {
            _syncState.value = SyncState.SYNCING
            backupManager.importFromLocal(uri)
                .onSuccess {
                    _syncState.value = SyncState.SUCCESS
                    _message.value = "导入成功"
                }
                .onFailure { e ->
                    _syncState.value = SyncState.ERROR
                    _message.value = "导入失败: ${e.message}"
                }
        }
    }

    fun exportLocal(onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            _syncState.value = SyncState.SYNCING
            try {
                val uri = backupManager.exportToLocal()
                _syncState.value = SyncState.SUCCESS
                _message.value = "导出成功"
                onResult(uri)
            } catch (e: Exception) {
                _syncState.value = SyncState.ERROR
                _message.value = "导出失败: ${e.message}"
                onResult(null)
            }
        }
    }

    fun clearMessage() {
        _message.value = null
        _syncState.value = SyncState.IDLE
    }
}
