package com.linghualive.flamekit.feature.reader.engine

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.linghualive.flamekit.core.theme.ReaderColors
import kotlinx.coroutines.launch

@Composable
fun PageFlipView(
    currentPage: TextPage?,
    nextPage: TextPage?,
    prevPage: TextPage?,
    config: PaginateConfig,
    readerColors: ReaderColors,
    flipMode: FlipMode,
    onNextPage: () -> Unit,
    onPrevPage: () -> Unit,
    onTapCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val renderer = remember { PageRenderer() }
    val simulationFlip = remember { SimulationPageFlip() }
    val coverFlip = remember { CoverPageFlip() }

    var viewWidth by remember { mutableFloatStateOf(0f) }
    var viewHeight by remember { mutableFloatStateOf(0f) }

    // Drag state
    var isDragging by remember { mutableStateOf(false) }
    var dragDirection by remember { mutableStateOf(0) } // -1 = next, 1 = prev
    val dragOffsetX = remember { Animatable(0f) }

    val textColor = readerColors.textColor.toArgb()
    val bgColor = readerColors.background.toArgb()
    val titleColor = readerColors.textColor.toArgb()

    // Pre-render bitmaps
    val currentBitmap = remember(currentPage, viewWidth, viewHeight, textColor, bgColor) {
        if (viewWidth <= 0 || viewHeight <= 0 || currentPage == null) null
        else createPageBitmap(currentPage, config, renderer, viewWidth.toInt(), viewHeight.toInt(), textColor, bgColor, titleColor)
    }

    val nextBitmap = remember(nextPage, viewWidth, viewHeight, textColor, bgColor) {
        if (viewWidth <= 0 || viewHeight <= 0 || nextPage == null) null
        else createPageBitmap(nextPage, config, renderer, viewWidth.toInt(), viewHeight.toInt(), textColor, bgColor, titleColor)
    }

    val prevBitmap = remember(prevPage, viewWidth, viewHeight, textColor, bgColor) {
        if (viewWidth <= 0 || viewHeight <= 0 || prevPage == null) null
        else createPageBitmap(prevPage, config, renderer, viewWidth.toInt(), viewHeight.toInt(), textColor, bgColor, titleColor)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                viewWidth = it.width.toFloat()
                viewHeight = it.height.toFloat()
            }
            .pointerInput(currentPage, nextPage, prevPage) {
                detectTapGestures { offset ->
                    val third = size.width / 3f
                    val centerYRange = size.height * 0.2f..size.height * 0.8f
                    when {
                        offset.y in centerYRange && offset.x in third..(third * 2) -> onTapCenter()
                        offset.x < third -> onPrevPage()
                        offset.x > third * 2 -> onNextPage()
                    }
                }
            }
            .pointerInput(currentPage, nextPage, prevPage, flipMode) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragDirection = 0
                    },
                    onDrag = { _, dragAmount ->
                        scope.launch {
                            val newOffset = (dragOffsetX.value + dragAmount.x)
                                .coerceIn(-viewWidth, viewWidth)
                            dragOffsetX.snapTo(newOffset)
                            if (dragDirection == 0 && kotlin.math.abs(newOffset) > 10f) {
                                dragDirection = if (newOffset < 0) -1 else 1
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        scope.launch {
                            val threshold = viewWidth * 0.3f
                            val offset = dragOffsetX.value
                            if (offset < -threshold && nextPage != null) {
                                dragOffsetX.animateTo(-viewWidth, tween(250))
                                onNextPage()
                                dragOffsetX.snapTo(0f)
                            } else if (offset > threshold && prevPage != null) {
                                dragOffsetX.animateTo(viewWidth, tween(250))
                                onPrevPage()
                                dragOffsetX.snapTo(0f)
                            } else {
                                dragOffsetX.animateTo(0f, tween(250))
                            }
                            dragDirection = 0
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        scope.launch {
                            dragOffsetX.animateTo(0f, tween(200))
                            dragDirection = 0
                        }
                    },
                )
            },
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas

        if (currentBitmap == null) {
            nativeCanvas.drawColor(bgColor)
            return@Canvas
        }

        val offset = dragOffsetX.value

        when {
            flipMode == FlipMode.SIMULATION && offset < 0 && nextBitmap != null -> {
                val flipState = simulationFlip.calculateFlipState(
                    touchX = viewWidth + offset,
                    touchY = viewHeight / 2,
                    pageWidth = viewWidth,
                    pageHeight = viewHeight,
                )
                simulationFlip.draw(nativeCanvas, currentBitmap, nextBitmap, flipState, viewWidth)
            }
            flipMode == FlipMode.SIMULATION && offset > 0 && prevBitmap != null -> {
                val flipState = simulationFlip.calculateFlipState(
                    touchX = offset,
                    touchY = viewHeight / 2,
                    pageWidth = viewWidth,
                    pageHeight = viewHeight,
                )
                simulationFlip.draw(nativeCanvas, prevBitmap, currentBitmap, flipState, viewWidth)
            }
            flipMode == FlipMode.COVER && offset < 0 && nextBitmap != null -> {
                coverFlip.draw(nativeCanvas, currentBitmap, nextBitmap, offset, viewWidth, viewHeight)
            }
            flipMode == FlipMode.COVER && offset > 0 && prevBitmap != null -> {
                coverFlip.draw(nativeCanvas, prevBitmap, currentBitmap, offset - viewWidth, viewWidth, viewHeight)
            }
            else -> {
                nativeCanvas.drawBitmap(currentBitmap, 0f, 0f, null)
            }
        }
    }
}

private fun createPageBitmap(
    page: TextPage,
    config: PaginateConfig,
    renderer: PageRenderer,
    width: Int,
    height: Int,
    textColor: Int,
    bgColor: Int,
    titleColor: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    renderer.render(canvas, page, config, textColor, bgColor, titleColor)
    return bitmap
}
