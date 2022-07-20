package net.matsudamper.zoomableimage

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

@Stable
public class ZoomableImageState {
    public var imageZoom: Float by mutableStateOf(1f)
    public var offsetX: Float by mutableStateOf(0f)
    public var offsetY: Float by mutableStateOf(0f)

    public companion object {
        public val Saver: Saver<ZoomableImageState, *> = listSaver(
            save = { listOf(it.imageZoom, it.offsetX, it.offsetY) },
            restore = { list ->
                ZoomableImageState().also {
                    it.imageZoom = list[0]
                    it.offsetX = list[1]
                    it.offsetY = list[2]
                }
            }
        )
    }
}

@Composable
public fun rememberZoomableImageState(): ZoomableImageState {
    return rememberSaveable(saver = ZoomableImageState.Saver) {
        ZoomableImageState()
    }
}

@Composable
public fun ZoomableImage(
    modifier: Modifier,
    state: ZoomableImageState = rememberZoomableImageState(),
    painter: Painter,
    maxZoomLevel: Float = 6.0f,
    contentDescription: String?
) {
    val imageSize by rememberUpdatedState(painter.intrinsicSize)
    var containerSize: IntSize? by remember {
        mutableStateOf(null)
    }
    val zoomTotal by remember {
        derivedStateOf { state.imageZoom }
    }
    val imageContainerSize: Size by remember {
        derivedStateOf {
            if (imageSize == Size.Unspecified) return@derivedStateOf Size.Unspecified
            val capturedContainerSize = containerSize ?: return@derivedStateOf Size.Unspecified
            val imageWidth =
                imageSize.width.takeIf { it > 0 } ?: return@derivedStateOf Size.Unspecified
            val imageHeight =
                imageSize.height.takeIf { it > 0 } ?: return@derivedStateOf Size.Unspecified

            val widthRatio = capturedContainerSize.width / imageWidth
            val heightRatio = capturedContainerSize.height / imageHeight

            val minRatio = min(
                widthRatio,
                heightRatio,
            )
            val result = if (widthRatio > heightRatio) {
                Size(
                    width = imageWidth * minRatio,
                    height = capturedContainerSize.height.toFloat(),
                )
            } else {
                Size(
                    width = capturedContainerSize.width.toFloat(),
                    height = imageHeight * minRatio,
                )
            }

            result
        }
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = zoomTotal
                scaleY = zoomTotal
                translationX = state.offsetX
                translationY = state.offsetY
            }
            .onSizeChanged {
                containerSize = it
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (imageContainerSize == Size.Unspecified) return@detectTransformGestures

                    state.imageZoom = run {
                        val newZoom = state.imageZoom * zoom
                        if (newZoom < 1f && zoom < 1) return@run 1f
                        if (newZoom > maxZoomLevel && zoom > 1) return@run maxZoomLevel
                        return@run newZoom
                    }

                    val tmpOffsetX = state.offsetX + (pan.x * zoomTotal)
                    val tmpOffsetY = state.offsetY + (pan.y * zoomTotal)
                    val capturedContainerSize = containerSize
                    if (capturedContainerSize == null) {
                        state.offsetX += (pan.x * zoomTotal)
                        state.offsetY += (pan.y * zoomTotal)
                        return@detectTransformGestures
                    }

                    val sizeDiffX = capturedContainerSize.width - imageContainerSize.width
                    val sizeDiffY = capturedContainerSize.height - imageContainerSize.height
                    val start = Offset(
                        x = tmpOffsetX - (imageContainerSize.width * (zoomTotal - 1) / 2f) + (sizeDiffX / 2),
                        y = tmpOffsetY - (imageContainerSize.height * (zoomTotal - 1) / 2f) + (sizeDiffY / 2),
                    )
                    val end = Offset(
                        x = start.x + (imageContainerSize.width * zoomTotal),
                        y = start.y + (imageContainerSize.height * zoomTotal),
                    )
                    state.offsetY = run offsetY@{
                        if (imageContainerSize.height * zoomTotal <= capturedContainerSize.height) {
                            0f
                        } else {
                            if (start.y >= 0 && pan.y > 0) {
                                val imageHeight = imageContainerSize.height * zoomTotal
                                val containerHeight = capturedContainerSize.height
                                val diffHeight = imageHeight - containerHeight
                                return@offsetY diffHeight / 2
                            }

                            if (
                                end.y <= capturedContainerSize.height &&
                                pan.y < 0
                            ) {
                                val imageHeight = imageContainerSize.height * zoomTotal
                                val containerHeight = capturedContainerSize.height
                                val diffHeight = imageHeight - containerHeight
                                return@offsetY -diffHeight / 2
                            }
                            tmpOffsetY
                        }
                    }

                    state.offsetX = run offsetX@{
                        if (imageContainerSize.width * zoomTotal <= capturedContainerSize.width) {
                            0f
                        } else {
                            if (start.x >= 0 && pan.x > 0) {
                                val imageWidth = imageContainerSize.width * zoomTotal
                                val containerWidth = capturedContainerSize.width
                                val diffWidth = imageWidth - containerWidth
                                return@offsetX diffWidth / 2
                            }

                            if (
                                end.x <= capturedContainerSize.width &&
                                pan.x < 0
                            ) {
                                val imageWidth = imageContainerSize.width * zoomTotal
                                val containerWidth = capturedContainerSize.width
                                val diffWidth = imageWidth - containerWidth
                                return@offsetX -diffWidth / 2
                            }
                            tmpOffsetX
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            modifier = Modifier,
            painter = painter,
            contentDescription = contentDescription
        )
    }
}
