package com.linghualive.flamekit.feature.source.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linghualive.flamekit.feature.source.domain.model.SearchResult
import com.linghualive.flamekit.feature.source.domain.repository.BookSourceRepository
import com.linghualive.flamekit.feature.source.engine.SourceExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BookSourceRepository,
    private val sourceExecutor: SourceExecutor,
) : ViewModel() {

    private val initialKeyword: String = savedStateHandle["keyword"] ?: ""

    private val _query = MutableStateFlow(initialKeyword)
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private var searchJob: Job? = null

    init {
        if (initialKeyword.isNotBlank()) {
            search(initialKeyword)
        }
    }

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun search(keyword: String = _query.value) {
        if (keyword.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _results.value = emptyList()

            val sources = repository.getEnabledSources()
                .filter { it.lastCheckSuccess || it.lastCheckTime == 0L }
                .sortedByDescending { it.lastCheckTime }
            val semaphore = Semaphore(5) // Max 5 concurrent requests
            val allResults = mutableListOf<SearchResult>()

            supervisorScope {
                val jobs = sources.map { source ->
                    async {
                        semaphore.acquire()
                        try {
                            withTimeout(10_000) {
                                val results = sourceExecutor.search(source, keyword)
                                synchronized(allResults) {
                                    allResults.addAll(results)
                                    _results.value = allResults.toList()
                                }
                            }
                        } catch (_: Exception) {
                            // Timeout or source error — skip
                        } finally {
                            semaphore.release()
                        }
                    }
                }
                jobs.awaitAll()
            }

            _isSearching.value = false
        }
    }
}
