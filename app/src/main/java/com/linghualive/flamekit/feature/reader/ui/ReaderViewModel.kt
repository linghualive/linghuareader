package com.linghualive.flamekit.feature.reader.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linghualive.flamekit.core.datastore.CustomReaderTheme
import com.linghualive.flamekit.core.datastore.PageMode
import com.linghualive.flamekit.core.datastore.ReaderThemeType
import com.linghualive.flamekit.core.datastore.ReadingPreferences
import com.linghualive.flamekit.core.datastore.ReadingPrefsDataStore
import com.linghualive.flamekit.core.datastore.ScreenOrientation
import com.linghualive.flamekit.feature.reader.domain.model.Bookmark
import com.linghualive.flamekit.feature.reader.domain.model.Note
import com.linghualive.flamekit.feature.reader.domain.model.ReaderState
import com.linghualive.flamekit.feature.reader.domain.model.SearchResult
import com.linghualive.flamekit.feature.reader.domain.repository.BookmarkRepository
import com.linghualive.flamekit.feature.reader.domain.repository.NoteRepository
import com.linghualive.flamekit.feature.reader.domain.repository.ReaderRepository
import com.linghualive.flamekit.feature.reader.engine.ContentSearchEngine
import com.linghualive.flamekit.feature.reader.engine.AutoPageTurner
import com.linghualive.flamekit.feature.reader.engine.ContentCleaner
import com.linghualive.flamekit.feature.reader.engine.FontInfo
import com.linghualive.flamekit.feature.reader.engine.FontManager
import com.linghualive.flamekit.feature.reader.engine.ReadingTracker
import com.linghualive.flamekit.feature.reader.engine.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val readerRepository: ReaderRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val noteRepository: NoteRepository,
    private val contentSearchEngine: ContentSearchEngine,
    private val readingPrefsDataStore: ReadingPrefsDataStore,
    val fontManager: FontManager,
    val ttsManager: TtsManager,
    private val readingTracker: ReadingTracker,
    private val contentCleaner: ContentCleaner,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bookId: Long = savedStateHandle["bookId"] ?: 0L

    private val _readerState = MutableStateFlow(ReaderState(bookId = bookId))
    val readerState: StateFlow<ReaderState> = _readerState.asStateFlow()

    val readingPrefs: StateFlow<ReadingPreferences> = readingPrefsDataStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingPreferences())

    val bookmarks: StateFlow<List<Bookmark>> = bookmarkRepository.getBookmarks(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ttsState: StateFlow<TtsManager.TtsState> = ttsManager.state

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    val notes: StateFlow<List<Note>> = noteRepository.getNotesByBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val autoPageTurner = AutoPageTurner()
    private var contentSearchJob: Job? = null

    init {
        ttsManager.initialize()
        readingTracker.startTracking(bookId)
        loadBook()
    }

    fun loadBook() {
        viewModelScope.launch {
            _readerState.update { it.copy(isLoading = true) }

            val title = readerRepository.getBookTitle(bookId)
            val format = readerRepository.getBookFormat(bookId)
            val filePath = readerRepository.getBookFilePath(bookId)
            val chapters = readerRepository.loadChapters(bookId)
            val progress = readerRepository.getProgress(bookId)

            val chapterIndex = progress?.first ?: 0
            val scrollPosition = progress?.second ?: 0
            val rawContent = readerRepository.loadChapterContent(bookId, chapterIndex)
            val content = cleanContentIfEnabled(rawContent)

            _readerState.update {
                it.copy(
                    bookTitle = title,
                    bookFormat = format,
                    bookFilePath = filePath,
                    chapters = chapters,
                    currentChapterIndex = chapterIndex,
                    currentChapterContent = content,
                    scrollPosition = scrollPosition,
                    isLoading = false,
                )
            }
        }
    }

    fun navigateToChapter(index: Int) {
        viewModelScope.launch {
            saveCurrentProgress()
            readingTracker.onChapterChanged()
            _readerState.update { it.copy(isLoading = true, showChapterList = false) }

            val rawContent = readerRepository.loadChapterContent(bookId, index)
            val content = cleanContentIfEnabled(rawContent)
            _readerState.update {
                it.copy(
                    currentChapterIndex = index,
                    currentChapterContent = content,
                    scrollPosition = 0,
                    isLoading = false,
                )
            }
        }
    }

    fun nextChapter() {
        val state = _readerState.value
        if (state.currentChapterIndex < state.chapters.size - 1) {
            navigateToChapter(state.currentChapterIndex + 1)
        }
    }

    fun prevChapter() {
        val state = _readerState.value
        if (state.currentChapterIndex > 0) {
            navigateToChapter(state.currentChapterIndex - 1)
        }
    }

    fun updateScrollPosition(position: Int) {
        _readerState.update { it.copy(scrollPosition = position) }
        readingTracker.onPageTurned()
    }

    fun toggleToolbar() {
        _readerState.update { it.copy(showToolbar = !it.showToolbar) }
    }

    fun hideToolbar() {
        _readerState.update { it.copy(showToolbar = false) }
    }

    fun toggleChapterList() {
        _readerState.update { it.copy(showChapterList = !it.showChapterList) }
    }

    fun toggleSettingsPanel() {
        _readerState.update { it.copy(showSettingsPanel = !it.showSettingsPanel) }
    }

    fun dismissChapterList() {
        _readerState.update { it.copy(showChapterList = false) }
    }

    fun dismissSettingsPanel() {
        _readerState.update { it.copy(showSettingsPanel = false) }
    }

    fun addBookmark() {
        val state = _readerState.value
        val chapterTitle = state.chapters.getOrNull(state.currentChapterIndex)?.title ?: ""
        viewModelScope.launch {
            bookmarkRepository.addBookmark(
                Bookmark(
                    bookId = bookId,
                    chapterIndex = state.currentChapterIndex,
                    position = state.scrollPosition,
                    title = chapterTitle,
                )
            )
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            bookmarkRepository.deleteBookmark(bookmark)
        }
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

    fun updateFontFamily(fontFamily: String) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(fontFamily = fontFamily) }
        }
    }

    fun updateCustomTheme(theme: CustomReaderTheme) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(customTheme = theme) }
        }
    }

    fun getAvailableFonts(): List<FontInfo> = fontManager.getAllFonts()

    // Brightness
    fun updateBrightness(brightness: Float) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(brightness = brightness) }
        }
    }

    // Keep screen on
    fun updateKeepScreenOn(keepOn: Boolean) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(keepScreenOn = keepOn) }
        }
    }

    // Volume key page turn
    fun updateVolumeKeyPageTurn(enabled: Boolean) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(volumeKeyPageTurn = enabled) }
        }
    }

    // Auto page turn
    fun toggleAutoPageTurn() {
        val prefs = readingPrefs.value
        val isRunning = autoPageTurner.isRunning.value
        if (isRunning) {
            autoPageTurner.stop()
            _readerState.update { it.copy(isAutoPageTurning = false) }
        } else {
            autoPageTurner.start(
                intervalMs = prefs.autoPageTurnInterval,
                scope = viewModelScope,
                onNextPage = ::nextChapter,
            )
            _readerState.update { it.copy(isAutoPageTurning = true) }
        }
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(autoPageTurn = !isRunning) }
        }
    }

    fun updateAutoPageTurnInterval(intervalMs: Long) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(autoPageTurnInterval = intervalMs) }
        }
        // Restart if currently running
        if (autoPageTurner.isRunning.value) {
            autoPageTurner.stop()
            autoPageTurner.start(
                intervalMs = intervalMs,
                scope = viewModelScope,
                onNextPage = ::nextChapter,
            )
        }
    }

    fun pauseAutoPageTurn() {
        if (autoPageTurner.isRunning.value) {
            autoPageTurner.stop()
            _readerState.update { it.copy(isAutoPageTurning = false) }
        }
    }

    // Screen orientation
    fun updateScreenOrientation(orientation: ScreenOrientation) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(screenOrientation = orientation) }
        }
    }

    // Content cleaning
    fun updateContentCleanEnabled(enabled: Boolean) {
        viewModelScope.launch {
            readingPrefsDataStore.update { it.copy(contentCleanEnabled = enabled) }
            // Re-clean current content
            val rawContent = readerRepository.loadChapterContent(
                bookId, _readerState.value.currentChapterIndex
            )
            val content = if (enabled) {
                contentCleaner.clean(rawContent, readingPrefs.value.customCleanRules)
            } else {
                rawContent
            }
            _readerState.update { it.copy(currentChapterContent = content) }
        }
    }

    private suspend fun cleanContentIfEnabled(content: String): String {
        val prefs = readingPrefs.value
        return if (prefs.contentCleanEnabled) {
            contentCleaner.clean(content, prefs.customCleanRules)
        } else {
            content
        }
    }

    // Search methods
    fun toggleSearchSheet() {
        _readerState.update { it.copy(showSearchSheet = !it.showSearchSheet) }
    }

    fun dismissSearchSheet() {
        _readerState.update { it.copy(showSearchSheet = false) }
        _searchResults.value = emptyList()
    }

    fun searchContent(keyword: String) {
        contentSearchJob?.cancel()
        contentSearchJob = viewModelScope.launch {
            _readerState.update { it.copy(isSearching = true) }
            val results = contentSearchEngine.search(bookId, keyword)
            _searchResults.value = results
            _readerState.update { it.copy(isSearching = false) }
        }
    }

    fun navigateToSearchResult(result: SearchResult) {
        _readerState.update { it.copy(showSearchSheet = false) }
        navigateToChapter(result.chapterIndex)
    }

    // Notes methods
    fun toggleNoteList() {
        _readerState.update { it.copy(showNoteList = !it.showNoteList) }
    }

    fun dismissNoteList() {
        _readerState.update { it.copy(showNoteList = false) }
    }

    fun showAddNoteDialog(selectedText: String, startPos: Int, endPos: Int) {
        _readerState.update {
            it.copy(
                showAddNoteDialog = true,
                selectedTextForNote = selectedText,
                selectedTextStartPos = startPos,
                selectedTextEndPos = endPos,
            )
        }
    }

    fun dismissAddNoteDialog() {
        _readerState.update { it.copy(showAddNoteDialog = false) }
    }

    fun addNote(noteContent: String?, highlightColor: Long) {
        val state = _readerState.value
        viewModelScope.launch {
            noteRepository.addNote(
                bookId = bookId,
                chapterIndex = state.currentChapterIndex,
                startPos = state.selectedTextStartPos,
                endPos = state.selectedTextEndPos,
                selectedText = state.selectedTextForNote,
                noteContent = noteContent,
                color = highlightColor,
            )
            _readerState.update { it.copy(showAddNoteDialog = false) }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.deleteNote(note.id)
        }
    }

    fun navigateToNote(note: Note) {
        _readerState.update { it.copy(showNoteList = false) }
        navigateToChapter(note.chapterIndex)
    }

    // TTS methods
    fun toggleTtsPanel() {
        _readerState.update { it.copy(showTtsPanel = !it.showTtsPanel) }
    }

    fun dismissTtsPanel() {
        ttsManager.stop()
        _readerState.update { it.copy(showTtsPanel = false) }
    }

    fun startTts() {
        val content = _readerState.value.currentChapterContent
        if (content.isNotEmpty()) {
            ttsManager.speak(content)
        }
    }

    fun pauseTts() {
        ttsManager.pause()
    }

    fun resumeTts() {
        ttsManager.resume()
    }

    fun stopTts() {
        ttsManager.stop()
    }

    fun setTtsSpeed(speed: Float) {
        ttsManager.setSpeed(speed)
    }

    fun setTtsPitch(pitch: Float) {
        ttsManager.setPitch(pitch)
    }

    // Reading tracker lifecycle
    fun onPauseTracking() {
        readingTracker.pause()
        pauseAutoPageTurn()
    }

    fun onResumeTracking() {
        readingTracker.resume()
    }

    private suspend fun saveCurrentProgress() {
        val state = _readerState.value
        if (state.bookId > 0) {
            readerRepository.saveProgress(
                bookId = state.bookId,
                chapterIndex = state.currentChapterIndex,
                scrollPosition = state.scrollPosition,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
        autoPageTurner.stop()
        val state = _readerState.value
        if (state.bookId > 0 && !state.isLoading) {
            kotlinx.coroutines.runBlocking {
                readingTracker.stopTracking()
                readerRepository.saveProgress(
                    bookId = state.bookId,
                    chapterIndex = state.currentChapterIndex,
                    scrollPosition = state.scrollPosition,
                )
            }
        }
    }
}
