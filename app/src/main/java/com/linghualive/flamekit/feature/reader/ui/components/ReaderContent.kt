package com.linghualive.flamekit.feature.reader.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linghualive.flamekit.core.datastore.PageMode
import com.linghualive.flamekit.core.datastore.ReadingPreferences
import com.linghualive.flamekit.core.theme.ReaderColors
import com.linghualive.flamekit.feature.reader.engine.FlipMode
import com.linghualive.flamekit.feature.reader.engine.PageFlipView
import com.linghualive.flamekit.feature.reader.engine.PaginateConfig
import com.linghualive.flamekit.feature.reader.engine.TextPaginator
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ReaderContent(
    content: String,
    readerPrefs: ReadingPreferences,
    readerColors: ReaderColors,
    pageMode: PageMode,
    scrollPosition: Int,
    onScrollPositionChanged: (Int) -> Unit,
    onTapCenter: () -> Unit,
    modifier: Modifier = Modifier,
    chapterIndex: Int = 0,
    chapterTitle: String = "",
) {
    when (pageMode) {
        PageMode.SCROLL -> ScrollReaderContent(
            content = content,
            readerPrefs = readerPrefs,
            readerColors = readerColors,
            scrollPosition = scrollPosition,
            onScrollPositionChanged = onScrollPositionChanged,
            onTapCenter = onTapCenter,
            modifier = modifier,
        )
        PageMode.HORIZONTAL_FLIP -> PagerReaderContent(
            content = content,
            readerPrefs = readerPrefs,
            readerColors = readerColors,
            scrollPosition = scrollPosition,
            onScrollPositionChanged = onScrollPositionChanged,
            onTapCenter = onTapCenter,
            modifier = modifier,
        )
        PageMode.SIMULATION_FLIP -> CanvasReaderContent(
            content = content,
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            readerPrefs = readerPrefs,
            readerColors = readerColors,
            flipMode = FlipMode.SIMULATION,
            scrollPosition = scrollPosition,
            onScrollPositionChanged = onScrollPositionChanged,
            onTapCenter = onTapCenter,
            modifier = modifier,
        )
        PageMode.COVER_FLIP -> CanvasReaderContent(
            content = content,
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            readerPrefs = readerPrefs,
            readerColors = readerColors,
            flipMode = FlipMode.COVER,
            scrollPosition = scrollPosition,
            onScrollPositionChanged = onScrollPositionChanged,
            onTapCenter = onTapCenter,
            modifier = modifier,
        )
    }
}

@Composable
private fun ScrollReaderContent(
    content: String,
    readerPrefs: ReadingPreferences,
    readerColors: ReaderColors,
    scrollPosition: Int,
    onScrollPositionChanged: (Int) -> Unit,
    onTapCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paragraphs = remember(content) {
        content.split("\n").filter { it.isNotBlank() }
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = scrollPosition)

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { onScrollPositionChanged(it) }
    }

    val textStyle = TextStyle(
        fontSize = readerPrefs.fontSize.sp,
        lineHeight = (readerPrefs.fontSize * readerPrefs.lineSpacingMultiplier).sp,
        color = readerColors.textColor,
        textIndent = TextIndent(firstLine = (readerPrefs.paragraphIndent * readerPrefs.fontSize).sp),
    )

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onTapCenter) {
                detectTapGestures { offset ->
                    val height = size.height.toFloat()
                    val tapY = offset.y
                    if (tapY > height * 0.2f && tapY < height * 0.8f) {
                        onTapCenter()
                    }
                }
            },
        contentPadding = PaddingValues(
            horizontal = readerPrefs.marginHorizontal.dp,
            vertical = readerPrefs.marginVertical.dp,
        ),
    ) {
        itemsIndexed(paragraphs) { _, paragraph ->
            Text(
                text = paragraph.trim(),
                style = textStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagerReaderContent(
    content: String,
    readerPrefs: ReadingPreferences,
    readerColors: ReaderColors,
    scrollPosition: Int,
    onScrollPositionChanged: (Int) -> Unit,
    onTapCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val pageHeightPx = with(density) { maxHeight.toPx() }
        val pageWidthPx = with(density) { maxWidth.toPx() }
        val marginH = with(density) { readerPrefs.marginHorizontal.dp.toPx() }
        val marginV = with(density) { readerPrefs.marginVertical.dp.toPx() }

        val usableHeight = pageHeightPx - marginV * 2
        val lineHeightSp = readerPrefs.fontSize * readerPrefs.lineSpacingMultiplier
        val lineHeightPx = with(density) { lineHeightSp.sp.toPx() }
        val linesPerPage = (usableHeight / lineHeightPx).toInt().coerceAtLeast(1)

        val usableWidth = pageWidthPx - marginH * 2
        val charWidthPx = with(density) { readerPrefs.fontSize.sp.toPx() * 0.55f }
        val charsPerLine = (usableWidth / charWidthPx).toInt().coerceAtLeast(1)
        val charsPerPage = linesPerPage * charsPerLine

        val pages = remember(content, charsPerPage) {
            if (content.isEmpty()) listOf("")
            else content.chunked(charsPerPage)
        }

        val pagerState = rememberPagerState(
            initialPage = scrollPosition.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
            pageCount = { pages.size },
        )

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { onScrollPositionChanged(it) }
        }

        val textStyle = TextStyle(
            fontSize = readerPrefs.fontSize.sp,
            lineHeight = lineHeightSp.sp,
            color = readerColors.textColor,
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onTapCenter) {
                    detectTapGestures { offset ->
                        val height = size.height.toFloat()
                        val tapY = offset.y
                        if (tapY > height * 0.2f && tapY < height * 0.8f) {
                            onTapCenter()
                        }
                    }
                },
        ) { page ->
            Text(
                text = pages.getOrElse(page) { "" },
                style = textStyle,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = readerPrefs.marginHorizontal.dp,
                        vertical = readerPrefs.marginVertical.dp,
                    ),
            )
        }
    }
}

@Composable
private fun CanvasReaderContent(
    content: String,
    chapterIndex: Int,
    chapterTitle: String,
    readerPrefs: ReadingPreferences,
    readerColors: ReaderColors,
    flipMode: FlipMode,
    scrollPosition: Int,
    onScrollPositionChanged: (Int) -> Unit,
    onTapCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val pageWidthPx = with(density) { maxWidth.toPx() }.toInt()
        val pageHeightPx = with(density) { maxHeight.toPx() }.toInt()
        val marginH = with(density) { readerPrefs.marginHorizontal.dp.toPx() }.toInt()
        val marginV = with(density) { readerPrefs.marginVertical.dp.toPx() }.toInt()
        val fontSizePx = with(density) { readerPrefs.fontSize.sp.toPx() }
        val titleFontSizePx = fontSizePx * 1.4f

        val config = remember(pageWidthPx, pageHeightPx, readerPrefs) {
            PaginateConfig(
                pageWidth = pageWidthPx,
                pageHeight = pageHeightPx,
                fontSize = fontSizePx,
                lineSpacingMultiplier = readerPrefs.lineSpacingMultiplier,
                paragraphIndent = readerPrefs.paragraphIndent,
                marginHorizontal = marginH,
                marginVertical = marginV,
                titleFontSize = titleFontSizePx,
            )
        }

        val paginator = remember { TextPaginator() }
        val pages = remember(content, config, chapterIndex, chapterTitle) {
            paginator.paginate(content, chapterIndex, chapterTitle, config)
        }

        var currentPageIndex by remember(pages) {
            mutableIntStateOf(scrollPosition.coerceIn(0, (pages.size - 1).coerceAtLeast(0)))
        }

        val currentPage = pages.getOrNull(currentPageIndex)
        val nextPage = pages.getOrNull(currentPageIndex + 1)
        val prevPage = pages.getOrNull(currentPageIndex - 1)

        PageFlipView(
            currentPage = currentPage,
            nextPage = nextPage,
            prevPage = prevPage,
            config = config,
            readerColors = readerColors,
            flipMode = flipMode,
            onNextPage = {
                if (currentPageIndex < pages.size - 1) {
                    currentPageIndex++
                    onScrollPositionChanged(currentPageIndex)
                }
            },
            onPrevPage = {
                if (currentPageIndex > 0) {
                    currentPageIndex--
                    onScrollPositionChanged(currentPageIndex)
                }
            },
            onTapCenter = onTapCenter,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
