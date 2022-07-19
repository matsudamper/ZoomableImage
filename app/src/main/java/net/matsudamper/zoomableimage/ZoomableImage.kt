package net.matsudamper.zoomableimage

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
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
    state: ZoomableImageState = ZoomableImageState(),
    painter: Painter,
    maxZoomLevel: Float = 5.0f,
    contentDescription: String?
) {
    val imageSize by rememberUpdatedState(painter.intrinsicSize)
    var containerSize: IntSize? by remember {
        mutableStateOf(null)
    }
    val sizeRate by remember {
        derivedStateOf {
            if (imageSize == Size.Unspecified) return@derivedStateOf 1f
            val capturedContainerSize = containerSize ?: return@derivedStateOf 1f
            val imageWidth = imageSize.width.takeIf { it > 0 } ?: return@derivedStateOf 1f
            val imageHeight = imageSize.height.takeIf { it > 0 } ?: return@derivedStateOf 1f

            val ra = min(
                capturedContainerSize.width / imageWidth,
                capturedContainerSize.height / imageHeight,
            )

            ra
        }
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = (1 / maxZoomLevel) * sizeRate * state.imageZoom
                scaleY = (1 / maxZoomLevel) * sizeRate * state.imageZoom
                translationX = state.offsetX / maxZoomLevel
                translationY = state.offsetY / maxZoomLevel
            },
        contentAlignment = Alignment.Center,
    ) {
        Layout(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (imageSize == Size.Unspecified) return@detectTransformGestures

                        state.imageZoom = run {
                            val newZoom = state.imageZoom * zoom
                            if (newZoom < 1f && zoom < 1) return@run 1f
                            if (newZoom > maxZoomLevel && zoom > 1) return@run maxZoomLevel
                            return@run newZoom
                        }

                        val tmpOffsetX = state.offsetX + (pan.x * state.imageZoom)
                        val tmpOffsetY = state.offsetY + (pan.y * state.imageZoom)
                        val capturedContainerSize = containerSize
                        if (capturedContainerSize == null) {
                            state.offsetX += (pan.x * state.imageZoom)
                            state.offsetY += (pan.y * state.imageZoom)
                            return@detectTransformGestures
                        }

                        val sizeDiffX = capturedContainerSize.width - imageSize.width
                        val sizeDiffY = capturedContainerSize.height - imageSize.height
                        val start = Offset(
                            x = tmpOffsetX - (imageSize.width * (state.imageZoom - 1) / 2f) + sizeDiffX / 2,
                            y = tmpOffsetY - (imageSize.height * (state.imageZoom - 1) / 2f) + sizeDiffY / 2,
                        )
                        val end = Offset(
                            x = start.x + (imageSize.width * state.imageZoom),
                            y = start.y + (imageSize.height * state.imageZoom),
                        )

                        state.offsetY = run offsetY@{
                            if (imageSize.height * state.imageZoom <= capturedContainerSize.height) {
                                0f
                            } else {
                                if (start.y >= 0 && pan.y > 0) {
                                    val imageHeight = imageSize.height * state.imageZoom
                                    val containerHeight = capturedContainerSize.height
                                    val diffHeight = imageHeight - containerHeight
                                    return@offsetY diffHeight / 2
                                }

                                if (
                                    end.y <= capturedContainerSize.height &&
                                    pan.y < 0
                                ) {
                                    val imageHeight = imageSize.height * state.imageZoom
                                    val containerHeight = capturedContainerSize.height
                                    val diffHeight = imageHeight - containerHeight
                                    return@offsetY -diffHeight / 2
                                }
                                tmpOffsetY
                            }
                        }

                        state.offsetX = run offsetX@{
                            if (imageSize.width * state.imageZoom <= capturedContainerSize.width) {
                                0f
                            } else {
                                if (start.x >= 0 && pan.x > 0) {
                                    val imageWidth = imageSize.width * state.imageZoom
                                    val containerWidth = capturedContainerSize.width
                                    val diffWidth = imageWidth - containerWidth
                                    return@offsetX diffWidth / 2
                                }

                                if (
                                    end.x <= capturedContainerSize.width &&
                                    pan.x < 0
                                ) {
                                    val imageWidth = imageSize.width * state.imageZoom
                                    val containerWidth = capturedContainerSize.width
                                    val diffWidth = imageWidth - containerWidth
                                    return@offsetX -diffWidth / 2
                                }
                                tmpOffsetX
                            }
                        }
                    }
                },
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged {
                            containerSize = it
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
        ) { measurables, constraints ->
            val placeable = measurables.map {
                it.measure(
                    Constraints.fixed(
                        width = (constraints.maxWidth * maxZoomLevel).toInt(),
                        height = (constraints.maxHeight * maxZoomLevel).toInt(),
                    )
                )
            }.first()

            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }
}
