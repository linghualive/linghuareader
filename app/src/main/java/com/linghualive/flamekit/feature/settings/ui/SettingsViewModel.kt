package com.linghualive.flamekit.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linghualive.flamekit.core.datastore.PageMode
import com.linghualive.flamekit.core.datastore.ReaderThemeType
import com.linghualive.flamekit.core.datastore.ReadingPreferences
import com.linghualive.flamekit.core.datastore.ReadingPrefsDataStore
import com.linghualive.flamekit.core.datastore.ThemeMode
import com.linghualive.flamekit.feature.update.data.AppUpdateChecker
import com.linghualive.flamekit.feature.update.domain.model.AppRelease
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val readingPrefsDataStore: ReadingPrefsDataStore,
    private val appUpdateChecker: AppUpdateChecker,
) : ViewModel() {

    val readingPrefs: StateFlow<ReadingPreferences> = readingPrefsDataStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingPreferences())

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun checkForUpdate() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            try {
                val release = appUpdateChecker.checkForUpdate()
                _updateState.value = if (release != null) {
                    UpdateState.Available(release)
                } else {
                    UpdateState.UpToDate
                }
            } catch (_: Exception) {
                _updateState.value = UpdateState.Idle
            }
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }

    sealed interface UpdateState {
        data object Idle : UpdateState
        data object Checking : UpdateState
        data object UpToDate : UpdateState
        data class Available(val release: AppRelease) : UpdateState
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(fontSize = size) }
        }
    }

    fun updateLineSpacing(spacing: Float) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(lineSpacingMultiplier = spacing) }
        }
    }

    fun updateReaderTheme(theme: ReaderThemeType) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(readerTheme = theme) }
        }
    }

    fun updatePageMode(mode: PageMode) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(pageMode = mode) }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(themeMode = mode) }
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(dynamicColor = enabled) }
        }
    }
}
