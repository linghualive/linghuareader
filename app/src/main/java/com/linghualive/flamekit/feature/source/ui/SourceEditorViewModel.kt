package com.linghualive.flamekit.feature.source.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linghualive.flamekit.feature.source.domain.model.BookSource
import com.linghualive.flamekit.feature.source.domain.repository.BookSourceRepository
import com.linghualive.flamekit.feature.source.engine.SourceExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SourceEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BookSourceRepository,
    private val sourceExecutor: SourceExecutor,
) : ViewModel() {

    private val editSourceUrl: String? = savedStateHandle["sourceUrl"]
    val isEditing: Boolean = editSourceUrl != null

    private val _source = MutableStateFlow(BookSource(sourceUrl = "", sourceName = ""))
    val source: StateFlow<BookSource> = _source

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult

    init {
        if (editSourceUrl != null) {
            viewModelScope.launch {
                repository.getSourceByUrl(editSourceUrl)?.let { existing ->
                    _source.value = existing
                }
            }
        }
    }

    fun updateSource(source: BookSource) {
        _source.value = source
    }

    fun save() {
        viewModelScope.launch {
            val current = _source.value
            if (current.sourceUrl.isBlank() || current.sourceName.isBlank()) {
                _message.value = "书源名称和 URL 不能为空"
                return@launch
            }
            if (isEditing) {
                repository.updateSource(current)
            } else {
                repository.addSource(current)
            }
            _message.value = "保存成功"
        }
    }

    fun testSearch() {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null
            try {
                val results = sourceExecutor.search(_source.value, "斗破苍穹")
                _testResult.value = if (results.isNotEmpty()) {
                    "搜索成功: 找到 ${results.size} 个结果\n${results.first().bookName}"
                } else {
                    "搜索成功但无结果"
                }
            } catch (e: Exception) {
                _testResult.value = "搜索失败: ${e.message}"
            } finally {
                _isTesting.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
