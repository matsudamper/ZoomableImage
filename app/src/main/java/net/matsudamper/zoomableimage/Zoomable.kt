package net.matsudamper.zoomableimage

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.launch

/**
 * A zoomable layout that can handle zoom in and out with drag support.
 *
 * @param state the state object to be used to observe the [Zoomable] state.
 * @param modifier the modifier to apply to this layout.
 * @param doubleTapScale a function called on double tap gesture, will scale to returned value.
 * @param content a block which describes the content.
 */
@Composable
public fun Zoomable(
    modifier: Modifier = Modifier,
    state: ZoomableState = rememberZoomableState(),
    enable: Boolean = true,
    doubleTapScale: (() -> Float)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = modifier,
    ) {
        var childWidth by remember { mutableStateOf(0) }
        var childHeight by remember { mutableStateOf(0) }
        LaunchedEffect(
            childHeight,
            childWidth,
            state.scale,
        ) {
            val maxX = (childWidth * state.scale - constraints.maxWidth)
                .coerceAtLeast(0F) / 2F
            val maxY = (childHeight * state.scale - constraints.maxHeight)
                .coerceAtLeast(0F) / 2F
            state.updateBounds(maxX, maxY)
        }
        val transformableState = rememberTransformableState { zoomChange, _, _ ->
            if (enable) {
                scope.launch {
                    state.onZoomChange(zoomChange)
                }
            }
        }
        val doubleTapModifier = if (doubleTapScale != null && enable) {
            Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scope.launch {
                            state.animateScaleTo(doubleTapScale())
                        }
                    }
                )
            }
        } else {
            Modifier
        }
        Box(
            modifier = Modifier
                .detectDragAndFling(
                    enable = enable,
                    onDrag = { change, dragAmount ->
                        if (state.zooming) {
                            if (change.positionChange() != Offset.Zero) change.consume()
                            scope.launch {
                                state.drag(dragAmount)
                            }
                        }
                    },
                    onDragCancel = {

                    },
                    onDragEnd = { offset ->
                        if (state.zooming) {
                            scope.launch {
                                state.fling(offset)
                            }
                        }
                    },
                )
                .then(doubleTapModifier)
                .transformable(state = transformableState)
                .layout { measurable, constraints ->
                    val placeable =
                        measurable.measure(constraints = constraints)
                    childHeight = placeable.height
                    childWidth = placeable.width
                    layout(
                        width = constraints.maxWidth,
                        height = constraints.maxHeight
                    ) {
                        placeable.placeRelativeWithLayer(
                            (constraints.maxWidth - placeable.width) / 2,
                            (constraints.maxHeight - placeable.height) / 2
                        ) {
                            scaleX = state.scale
                            scaleY = state.scale
                            translationX = state.translateX
                            translationY = state.translateY
                        }
                    }
                }
        ) {
            content.invoke(this)
        }
    }
}


private fun Modifier.detectDragAndFling(
    enable: Boolean,
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: (fling: Offset) -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
): Modifier {
    return composed {
        if (enable.not()) return@composed this

        val velocityTracker = remember { VelocityTracker() }
        pointerInput(Unit) {
            forEachGesture {
                awaitPointerEventScope {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var drag: PointerInputChange?
                    do {
                        drag = awaitTouchSlopOrCancellation(down.id) { change, dragAmount ->
                            velocityTracker.addPosition(
                                change.uptimeMillis,
                                change.position
                            )
                            onDrag(change, dragAmount)
                        }
                    } while (drag != null && !drag.isConsumed)
                    if (drag != null) {
                        onDragStart.invoke(drag.position)
                        if (
                            !drag(drag.id) { change ->
                                velocityTracker.addPosition(
                                    change.uptimeMillis,
                                    change.position
                                )
                                onDrag(change, change.positionChange())
                            }
                        ) {
                            velocityTracker.resetTracking()
                            onDragCancel()
                        } else {
                            val velocity = velocityTracker.calculateVelocity()
                            onDragEnd(Offset(velocity.x, velocity.y))
                        }
                    }
                }
            }
        }
    }
}