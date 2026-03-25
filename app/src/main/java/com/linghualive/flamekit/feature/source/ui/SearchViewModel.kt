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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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
            _errorMessage.value = null

            val sources = repository.getEnabledSources()
                .filter { !it.searchUrl.isNullOrBlank() }

            if (sources.isEmpty()) {
                _errorMessage.value = "没有可用的书源，请先添加书源"
                _isSearching.value = false
                return@launch
            }

            val semaphore = Semaphore(5)
            val allResults = mutableListOf<SearchResult>()
            val mutex = Mutex()

            supervisorScope {
                val jobs = sources.map { source ->
                    async {
                        semaphore.acquire()
                        try {
                            withTimeout(10_000) {
                                val results = sourceExecutor.search(source, keyword)
                                mutex.withLock {
                                    allResults.addAll(results)
                                    _results.value = rankAndDeduplicate(allResults, keyword)
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

            // Final sort
            _results.value = rankAndDeduplicate(allResults, keyword)
            _isSearching.value = false
        }
    }

    private fun rankAndDeduplicate(
        results: List<SearchResult>,
        keyword: String,
    ): List<SearchResult> {
        val normalizedKeyword = normalize(keyword)

        // Deduplicate: keep the first result per (title, author) pair
        val seen = mutableSetOf<String>()
        val unique = results.filter { result ->
            val key = "${normalize(result.bookName)}|${normalize(result.author ?: "")}"
            seen.add(key)
        }

        // Score and sort by relevance
        return unique.sortedByDescending { result ->
            relevanceScore(normalize(result.bookName), normalizedKeyword)
        }
    }

    private fun relevanceScore(title: String, keyword: String): Int {
        return when {
            title == keyword -> 100                          // exact match
            title.startsWith(keyword) -> 80                  // starts with
            title.contains(keyword) -> 60                    // contains
            keyword.length >= 2 && fuzzyMatch(title, keyword) -> 40  // fuzzy match
            else -> 0
        }
    }

    private fun fuzzyMatch(title: String, keyword: String): Boolean {
        // Check if all characters of keyword appear in title in order
        var ki = 0
        for (ch in title) {
            if (ki < keyword.length && ch == keyword[ki]) ki++
        }
        if (ki == keyword.length) return true

        // Check if keyword shares ≥50% characters with title
        val commonChars = keyword.count { it in title }
        return commonChars.toFloat() / keyword.length >= 0.5f
    }

    private fun normalize(text: String): String {
        return text.trim()
            .lowercase()
            .replace(Regex("[\\s　]+"), "")  // remove all whitespace including CJK space
    }
}
