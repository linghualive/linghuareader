package com.linghualive.flamekit.feature.reader.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.flow.filter

@Composable
fun ReaderContent(
    content: String,
    readerPrefs: ReadingPreferences,
    readerColors: ReaderColors,
    pageMode: PageMode,
    scrollPosition: Int,
    onScrollPositionChanged: (Int) -> Unit,
    onTapCenter: () -> Unit,
    onScrollStarted: () -> Unit = {},
    modifier: Modifier = Modifier,
    chapterIndex: Int = 0,
    chapterTitle: String = "",
) {
    when (pageMode) {
        PageMode.SCROLL -> ScrollReaderContent(
            content = content,
            chapterTitle = chapterTitle,
            readerPrefs = readerPrefs,
            readerColors = readerColors,
            scrollPosition = scrollPosition,
            onScrollPositionChanged = onScrollPositionChanged,
            onTapCenter = onTapCenter,
            onScrollStarted = onScrollStarted,
            modifier = modifier,
        )
        PageMode.HORIZONTAL_FLIP -> PagerReaderContent(
            content = content,
            chapterTitle = chapterTitle,
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
    chapterTitle: String,
    readerPrefs: ReadingPreferences,
    readerColors: ReaderColors,
    scrollPosition: Int,
    onScrollPositionChanged: (Int) -> Unit,
    onTapCenter: () -> Unit,
    onScrollStarted: () -> Unit = {},
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

    // Auto-hide toolbar when scrolling starts
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { it }
            .collect { onScrollStarted() }
    }

    val textStyle = TextStyle(
        fontSize = readerPrefs.fontSize.sp,
        lineHeight = (readerPrefs.fontSize * readerPrefs.lineSpacingMultiplier).sp,
        color = readerColors.textColor,
        textIndent = TextIndent(firstLine = (readerPrefs.paragraphIndent * readerPrefs.fontSize).sp),
        textAlign = TextAlign.Justify,
    )

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onTapCenter) {
                detectTapGestures { offset ->
                    val height = size.height.toFloat()
                    val width = size.width.toFloat()
                    val tapY = offset.y
                    val tapX = offset.x
                    if (tapY > height * 0.3f && tapY < height * 0.7f
                        && tapX > width * 0.2f && tapX < width * 0.8f) {
                        onTapCenter()
                    }
                }
            },
        contentPadding = PaddingValues(
            horizontal = readerPrefs.marginHorizontal.dp,
            vertical = readerPrefs.marginVertical.dp,
        ),
    ) {
        // Chapter title
        if (chapterTitle.isNotBlank()) {
            item {
                Text(
                    text = chapterTitle,
                    style = TextStyle(
                        fontSize = (readerPrefs.fontSize + 4).sp,
                        fontWeight = FontWeight.Bold,
                        color = readerColors.textColor,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = (readerPrefs.fontSize * 1.2f).dp),
                )
            }
        }
        itemsIndexed(paragraphs) { _, paragraph ->
            Text(
                text = paragraph.trim(),
                style = textStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = (readerPrefs.fontSize * 0.6f).dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagerReaderContent(
    content: String,
    chapterTitle: String,
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

        val usableHeight = pageHeightPx - marginV * 2 - with(density) { 24.dp.toPx() } // reserve space for page indicator
        val lineHeightSp = readerPrefs.fontSize * readerPrefs.lineSpacingMultiplier
        val lineHeightPx = with(density) { lineHeightSp.sp.toPx() }
        val linesPerPage = (usableHeight / lineHeightPx).toInt().coerceAtLeast(1)

        val usableWidth = pageWidthPx - marginH * 2
        // CJK characters are full-width (~1.0x font size), Latin ~0.55x. Use 0.85x as compromise.
        val charWidthPx = with(density) { readerPrefs.fontSize.sp.toPx() * 0.85f }
        val charsPerLine = (usableWidth / charWidthPx).toInt().coerceAtLeast(1)

        val pages = remember(content, linesPerPage, charsPerLine, chapterTitle) {
            paginateByLines(
                content = if (chapterTitle.isNotBlank()) "$chapterTitle\n\n$content" else content,
                linesPerPage = linesPerPage,
                charsPerLine = charsPerLine,
            )
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
            textIndent = TextIndent(firstLine = (readerPrefs.paragraphIndent * readerPrefs.fontSize).sp),
            textAlign = TextAlign.Justify,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(onTapCenter) {
                        detectTapGestures { offset ->
                            val height = size.height.toFloat()
                            val width = size.width.toFloat()
                            val tapY = offset.y
                            val tapX = offset.x
                            if (tapY > height * 0.3f && tapY < height * 0.7f
                                && tapX > width * 0.2f && tapX < width * 0.8f) {
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

            // Page indicator
            Text(
                text = "${pagerState.currentPage + 1} / ${pages.size}",
                style = MaterialTheme.typography.labelSmall,
                color = readerColors.secondaryText,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
            )
        }
    }
}

private fun paginateByLines(content: String, linesPerPage: Int, charsPerLine: Int): List<String> {
    if (content.isEmpty()) return listOf("")
    val paragraphs = content.split("\n")
    val allLines = mutableListOf<String>()

    for (paragraph in paragraphs) {
        val trimmed = paragraph.trim()
        if (trimmed.isEmpty()) continue
        // Wrap paragraph into lines
        var remaining = trimmed
        while (remaining.isNotEmpty()) {
            val lineLen = remaining.length.coerceAtMost(charsPerLine)
            allLines.add(remaining.substring(0, lineLen))
            remaining = remaining.substring(lineLen)
        }
        // Add empty line between paragraphs for spacing
        allLines.add("")
    }

    // Remove trailing empty lines
    while (allLines.isNotEmpty() && allLines.last().isEmpty()) {
        allLines.removeAt(allLines.size - 1)
    }

    if (allLines.isEmpty()) return listOf("")

    // Group lines into pages
    val pages = mutableListOf<String>()
    var i = 0
    while (i < allLines.size) {
        val endIndex = (i + linesPerPage).coerceAtMost(allLines.size)
        val pageLines = allLines.subList(i, endIndex)
        pages.add(pageLines.joinToString("\n"))
        i = endIndex
    }

    return pages.ifEmpty { listOf("") }
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
