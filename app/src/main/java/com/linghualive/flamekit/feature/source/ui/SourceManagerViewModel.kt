package com.linghualive.flamekit.feature.source.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linghualive.flamekit.feature.source.domain.model.BookSource
import com.linghualive.flamekit.feature.source.domain.model.SourceSubscription
import com.linghualive.flamekit.feature.source.domain.repository.BookSourceRepository
import com.linghualive.flamekit.feature.source.engine.SourceHealthChecker
import com.linghualive.flamekit.feature.source.engine.SubscriptionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class SourceManagerViewModel @Inject constructor(
    private val repository: BookSourceRepository,
    private val subscriptionManager: SubscriptionManager,
    private val healthChecker: SourceHealthChecker,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val sources: StateFlow<List<BookSource>> = repository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptions: StateFlow<List<SourceSubscription>> = subscriptionManager.getSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage

    // Batch selection
    private val _selectedUrls = MutableStateFlow<Set<String>>(emptySet())
    val selectedUrls: StateFlow<Set<String>> = _selectedUrls

    val isSelectionMode: Boolean get() = _selectedUrls.value.isNotEmpty()

    // Health check
    private val _healthResults = MutableStateFlow<Map<String, SourceHealthChecker.HealthResult>>(emptyMap())
    val healthResults: StateFlow<Map<String, SourceHealthChecker.HealthResult>> = _healthResults

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking

    private val _checkProgress = MutableStateFlow(0f)
    val checkProgress: StateFlow<Float> = _checkProgress

    private var healthCheckJob: Job? = null

    // Subscription
    private val _isRefreshingSubscription = MutableStateFlow(false)
    val isRefreshingSubscription: StateFlow<Boolean> = _isRefreshingSubscription

    init {
        loadDefaultSourcesIfNeeded()
    }

    private fun loadDefaultSourcesIfNeeded() {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("source_prefs", Context.MODE_PRIVATE)
                val storedVersion = prefs.getInt("default_sources_version", 0)
                if (storedVersion < DEFAULT_SOURCES_VERSION) {
                    val jsonStr = context.assets.open("default_sources.json")
                        .bufferedReader().use { it.readText() }
                    val sources = json.decodeFromString<List<BookSource>>(jsonStr)
                    repository.addSources(sources)
                    prefs.edit().putInt("default_sources_version", DEFAULT_SOURCES_VERSION).apply()
                }
            } catch (_: Exception) {
                // No default sources file or parse error
            }
        }
    }

    companion object {
        // Bump this number whenever default_sources.json is updated
        const val DEFAULT_SOURCES_VERSION = 2
    }

    fun importFromJson(jsonText: String) {
        viewModelScope.launch {
            try {
                val sources = json.decodeFromString<List<BookSource>>(jsonText)
                repository.addSources(sources)
                _importMessage.value = "成功导入 ${sources.size} 个书源"
            } catch (e: Exception) {
                try {
                    val source = json.decodeFromString<BookSource>(jsonText)
                    repository.addSource(source)
                    _importMessage.value = "成功导入 1 个书源"
                } catch (_: Exception) {
                    _importMessage.value = "导入失败: JSON 格式错误"
                }
            }
        }
    }

    fun toggleSourceEnabled(source: BookSource, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSource(source.copy(enabled = enabled))
        }
    }

    fun deleteSource(source: BookSource) {
        viewModelScope.launch {
            repository.deleteSource(source)
        }
    }

    fun clearImportMessage() {
        _importMessage.value = null
    }

    // ---- Batch Operations ----

    fun toggleSelection(sourceUrl: String) {
        _selectedUrls.value = _selectedUrls.value.toMutableSet().apply {
            if (contains(sourceUrl)) remove(sourceUrl) else add(sourceUrl)
        }
    }

    fun selectAll() {
        _selectedUrls.value = sources.value.map { it.sourceUrl }.toSet()
    }

    fun clearSelection() {
        _selectedUrls.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val urls = _selectedUrls.value.toList()
            if (urls.isNotEmpty()) {
                repository.deleteSources(urls)
                _selectedUrls.value = emptySet()
                _importMessage.value = "已删除 ${urls.size} 个书源"
            }
        }
    }

    fun enableSelected() {
        viewModelScope.launch {
            val urls = _selectedUrls.value.toList()
            if (urls.isNotEmpty()) {
                repository.updateEnabledBatch(urls, enabled = true)
                _importMessage.value = "已启用 ${urls.size} 个书源"
            }
        }
    }

    fun disableSelected() {
        viewModelScope.launch {
            val urls = _selectedUrls.value.toList()
            if (urls.isNotEmpty()) {
                repository.updateEnabledBatch(urls, enabled = false)
                _importMessage.value = "已禁用 ${urls.size} 个书源"
            }
        }
    }

    fun exportSelectedAsJson(): String {
        val selected = _selectedUrls.value
        val exportSources = sources.value.filter { it.sourceUrl in selected }
        return json.encodeToString(exportSources)
    }

    // ---- Health Check ----

    fun startHealthCheck() {
        healthCheckJob?.cancel()
        healthCheckJob = viewModelScope.launch {
            _isChecking.value = true
            _healthResults.value = emptyMap()
            _checkProgress.value = 0f

            val enabledSources = sources.value.filter { it.enabled }
            val total = enabledSources.size.coerceAtLeast(1)
            var checked = 0

            healthChecker.checkAllSources().collect { result ->
                checked++
                _healthResults.value = _healthResults.value + (result.sourceUrl to result)
                _checkProgress.value = checked.toFloat() / total
            }

            _isChecking.value = false
        }
    }

    fun autoDisableUnhealthy() {
        viewModelScope.launch {
            healthChecker.autoDisableUnhealthySources()
            _importMessage.value = "已自动禁用失效书源"
        }
    }

    // ---- Subscription ----

    fun addSubscription(name: String, url: String) {
        viewModelScope.launch {
            _isRefreshingSubscription.value = true
            val result = subscriptionManager.addSubscription(name, url)
            result.onSuccess {
                _importMessage.value = "添加订阅成功: ${it.sourceCount} 个书源"
            }.onFailure {
                _importMessage.value = "添加订阅失败: ${it.message}"
            }
            _isRefreshingSubscription.value = false
        }
    }

    fun refreshSubscription(subscription: SourceSubscription) {
        viewModelScope.launch {
            _isRefreshingSubscription.value = true
            val result = subscriptionManager.refreshSubscription(subscription)
            result.onSuccess { count ->
                _importMessage.value = "刷新成功: 更新 $count 个书源"
            }.onFailure {
                _importMessage.value = "刷新失败: ${it.message}"
            }
            _isRefreshingSubscription.value = false
        }
    }

    fun refreshAllSubscriptions() {
        viewModelScope.launch {
            _isRefreshingSubscription.value = true
            val result = subscriptionManager.refreshAll()
            result.onSuccess { map ->
                val total = map.values.sum()
                _importMessage.value = "刷新完成: 共更新 $total 个书源"
            }.onFailure {
                _importMessage.value = "刷新失败: ${it.message}"
            }
            _isRefreshingSubscription.value = false
        }
    }

    fun removeSubscription(url: String) {
        viewModelScope.launch {
            subscriptionManager.removeSubscription(url)
            _importMessage.value = "已删除订阅"
        }
    }
}
