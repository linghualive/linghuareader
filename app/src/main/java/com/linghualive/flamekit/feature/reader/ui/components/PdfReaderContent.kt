package com.linghualive.flamekit.feature.reader.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfReaderContent(
    bookFilePath: String,
    currentPage: Int,
    totalPages: Int,
    onPageChanged: (Int) -> Unit,
    onTapCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val rendererState = remember(bookFilePath) {
        try {
            val uri = android.net.Uri.parse(bookFilePath)
            val fd = context.contentResolver.openFileDescriptor(uri, "r")
            if (fd != null) {
                val renderer = PdfRenderer(fd)
                PdfRendererHolder(renderer, fd)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(rendererState) {
        onDispose {
            rendererState?.close()
        }
    }

    if (rendererState == null || totalPages <= 0) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0)),
        pageCount = { totalPages },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { onPageChanged(it) }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onTapCenter) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val tapX = offset.x
                    if (tapX > width * 0.3f && tapX < width * 0.7f) {
                        onTapCenter()
                    }
                }
            },
    ) { page ->
        PdfPage(
            rendererHolder = rendererState,
            pageIndex = page,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PdfPage(
    rendererHolder: PdfRendererHolder,
    pageIndex: Int,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pageIndex) {
        scale = 1f
        offset = Offset.Zero
        val bmp = rendererHolder.renderPage(pageIndex)
        bitmap = bmp
    }

    DisposableEffect(pageIndex) {
        onDispose {
            // Recycle old bitmap when page changes
            bitmap?.let { if (!it.isRecycled) it.recycle() }
            bitmap = null
        }
    }

    bitmap?.let { bmp ->
        if (bmp.isRecycled) return@let
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "PDF page ${pageIndex + 1}",
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale > 1f) offset + pan else Offset.Zero
                    }
                },
            contentScale = ContentScale.Fit,
        )
    } ?: Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private class PdfRendererHolder(
    private val renderer: PdfRenderer,
    private val fd: ParcelFileDescriptor,
) {
    private val mutex = Mutex()

    suspend fun renderPage(pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val page = renderer.openPage(pageIndex)
                val bmp = Bitmap.createBitmap(
                    page.width * 2,
                    page.height * 2,
                    Bitmap.Config.ARGB_8888,
                )
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bmp
            } catch (_: Exception) {
                null
            }
        }
    }

    fun close() {
        try {
            renderer.close()
        } catch (_: Exception) {
        }
        try {
            fd.close()
        } catch (_: Exception) {
        }
    }
}
