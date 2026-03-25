package com.linghualive.flamekit.feature.reader.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.linghualive.flamekit.core.datastore.ScreenOrientation
import com.linghualive.flamekit.core.theme.readerColorsFor
import com.linghualive.flamekit.feature.reader.ui.components.AddNoteDialog
import com.linghualive.flamekit.feature.reader.ui.components.ChapterListSheet
import com.linghualive.flamekit.feature.reader.ui.components.NoteListSheet
import com.linghualive.flamekit.feature.reader.ui.components.PdfReaderContent
import com.linghualive.flamekit.feature.reader.ui.components.ReaderContent
import com.linghualive.flamekit.feature.reader.ui.components.ReaderToolbar
import com.linghualive.flamekit.feature.reader.ui.components.ReadingSettingsPanel
import com.linghualive.flamekit.feature.reader.ui.components.SearchSheet
import com.linghualive.flamekit.feature.reader.ui.components.TtsControlPanel

@Composable
fun ReaderScreen(
    bookId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val readerState by viewModel.readerState.collectAsState()
    val prefs by viewModel.readingPrefs.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val readerColors = readerColorsFor(prefs.readerTheme, prefs.customTheme)

    val context = LocalContext.current
    val activity = context as? Activity

    // Lifecycle observer for reading tracker
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.onPauseTracking()
                Lifecycle.Event.ON_RESUME -> viewModel.onResumeTracking()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Keep screen on
    DisposableEffect(prefs.keepScreenOn) {
        if (prefs.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Brightness control
    LaunchedEffect(prefs.brightness) {
        activity?.window?.attributes = activity?.window?.attributes?.apply {
            screenBrightness = if (prefs.brightness >= 0f) {
                prefs.brightness
            } else {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            // Restore system brightness on exit
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    // Screen orientation lock
    LaunchedEffect(prefs.screenOrientation) {
        activity?.requestedOrientation = when (prefs.screenOrientation) {
            ScreenOrientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Pause auto page turn when settings panel or toolbar is shown
    LaunchedEffect(readerState.showSettingsPanel, readerState.showToolbar) {
        if (readerState.showSettingsPanel || readerState.showToolbar) {
            viewModel.pauseAutoPageTurn()
        }
    }

    // Immersive mode
    val view = LocalView.current
    DisposableEffect(readerState.showToolbar) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowInsetsControllerCompat(window, view)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (readerState.showToolbar) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Focus requester for volume key handling
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(readerColors.background)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (prefs.volumeKeyPageTurn && event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_VOLUME_DOWN -> {
                            viewModel.nextChapter()
                            true
                        }
                        KeyEvent.KEYCODE_VOLUME_UP -> {
                            viewModel.prevChapter()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
    ) {
        if (readerState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            // Reading content layer
            if (readerState.bookFormat == "pdf") {
                PdfReaderContent(
                    bookFilePath = readerState.bookFilePath,
                    currentPage = readerState.currentChapterIndex,
                    totalPages = readerState.chapters.size,
                    onPageChanged = { page ->
                        viewModel.navigateToChapter(page)
                    },
                    onTapCenter = viewModel::toggleToolbar,
                )
            } else {
                ReaderContent(
                    content = readerState.currentChapterContent,
                    readerPrefs = prefs,
                    readerColors = readerColors,
                    pageMode = prefs.pageMode,
                    scrollPosition = readerState.scrollPosition,
                    onScrollPositionChanged = viewModel::updateScrollPosition,
                    onTapCenter = viewModel::toggleToolbar,
                    onScrollStarted = viewModel::hideToolbar,
                    chapterIndex = readerState.currentChapterIndex,
                    chapterTitle = readerState.chapters.getOrNull(readerState.currentChapterIndex)?.title ?: "",
                )
            }

            // Status bar cover — prevents content from showing through the translucent status bar
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(readerColors.background),
            )

            // Toolbar overlay
            ReaderToolbar(
                visible = readerState.showToolbar,
                title = readerState.bookTitle,
                currentChapter = readerState.currentChapterIndex,
                totalChapters = readerState.chapters.size,
                onBack = onBack,
                onChapterList = viewModel::toggleChapterList,
                onSettings = viewModel::toggleSettingsPanel,
                onSeekChapter = viewModel::navigateToChapter,
            )
        }

        // Chapter list sheet
        if (readerState.showChapterList) {
            ChapterListSheet(
                chapters = readerState.chapters,
                currentIndex = readerState.currentChapterIndex,
                onChapterClick = viewModel::navigateToChapter,
                onDismiss = viewModel::dismissChapterList,
            )
        }

        // Settings panel
        if (readerState.showSettingsPanel) {
            ReadingSettingsPanel(
                prefs = prefs,
                onDismiss = viewModel::dismissSettingsPanel,
                onFontSizeChange = viewModel::updateFontSize,
                onLineSpacingChange = viewModel::updateLineSpacing,
                onThemeChange = viewModel::updateReaderTheme,
                onPageModeChange = viewModel::updatePageMode,
                fonts = viewModel.getAvailableFonts(),
                onFontChange = viewModel::updateFontFamily,
                onCustomThemeChange = viewModel::updateCustomTheme,
                onBrightnessChange = viewModel::updateBrightness,
                onKeepScreenOnChange = viewModel::updateKeepScreenOn,
                onVolumeKeyPageTurnChange = viewModel::updateVolumeKeyPageTurn,
                onAutoPageTurnChange = { viewModel.toggleAutoPageTurn() },
                onAutoPageTurnIntervalChange = viewModel::updateAutoPageTurnInterval,
                onScreenOrientationChange = viewModel::updateScreenOrientation,
                onContentCleanEnabledChange = viewModel::updateContentCleanEnabled,
            )
        }

        // TTS control panel
        if (readerState.showTtsPanel) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                TtsControlPanel(
                    ttsState = ttsState,
                    onPlay = {
                        if (ttsState.isPaused) viewModel.resumeTts() else viewModel.startTts()
                    },
                    onPause = viewModel::pauseTts,
                    onStop = viewModel::stopTts,
                    onSpeedChange = viewModel::setTtsSpeed,
                    onPitchChange = viewModel::setTtsPitch,
                    onDismiss = viewModel::dismissTtsPanel,
                )
            }
        }

        // Search sheet
        if (readerState.showSearchSheet) {
            SearchSheet(
                onSearch = viewModel::searchContent,
                searchResults = searchResults,
                isSearching = readerState.isSearching,
                onResultClick = viewModel::navigateToSearchResult,
                onDismiss = viewModel::dismissSearchSheet,
            )
        }

        // Note list sheet
        if (readerState.showNoteList) {
            NoteListSheet(
                notes = notes,
                onNoteClick = viewModel::navigateToNote,
                onDeleteNote = viewModel::deleteNote,
                onDismiss = viewModel::dismissNoteList,
            )
        }

        // Add note dialog
        if (readerState.showAddNoteDialog) {
            AddNoteDialog(
                selectedText = readerState.selectedTextForNote,
                onSave = viewModel::addNote,
                onDismiss = viewModel::dismissAddNoteDialog,
            )
        }
    }
}
