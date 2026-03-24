package com.linghualive.flamekit.feature.reader.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AutoPageTurner {
    private var job: Job? = null
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start(intervalMs: Long = 5000L, scope: CoroutineScope, onNextPage: () -> Unit) {
        stop()
        _isRunning.value = true
        job = scope.launch {
            while (isActive) {
                delay(intervalMs)
                onNextPage()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _isRunning.value = false
    }

    fun toggle(intervalMs: Long, scope: CoroutineScope, onNextPage: () -> Unit) {
        if (_isRunning.value) stop() else start(intervalMs, scope, onNextPage)
    }
}
