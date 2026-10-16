package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.apadmi.cmpdevicepreview.isWheelDeltaInPixels
import kotlin.math.exp

private const val MinZoom = 0.25f
private const val MaxZoom = 8f
private const val MinWheelZoomStep = 0.8f
private const val MaxWheelZoomStep = 1.25f

// Per unit of the wheel, which is a notch everywhere but the web, where a notch is about 100.
private val WheelPanPixels = if (isWheelDeltaInPixels) 1f else 48f
private val WheelZoomPerUnit = if (isWheelDeltaInPixels) 0.01f else 0.1f

// A zoom of 1 fits the device to the space; the view is a transform of that from the top-left corner.
@Stable
internal class PreviewViewport {
    var zoom by mutableFloatStateOf(1f)
        private set
    var pan by mutableStateOf(Offset.Zero)
        private set
    var size by mutableStateOf(Size.Zero)
        private set

    // Pixels per dp of the device at a zoom of 1, to tell how large the zoomed device is.
    var fitScale by mutableFloatStateOf(1f)

    val isTransformed: Boolean get() = zoom != 1f
    val canPan: Boolean get() = zoom > 1f

    fun resize(newSize: Size) {
        size = newSize
        pan = clamped(pan)
    }

    fun reset() {
        zoom = 1f
        pan = Offset.Zero
    }

    fun transformBy(
        centroid: Offset,
        panChange: Offset,
        zoomChange: Float
    ) {
        transformTo(centroid, panChange, zoom * zoomChange)
    }

    fun zoomTo(newZoom: Float, about: Offset = Offset(size.width / 2f, size.height / 2f)) {
        transformTo(about, Offset.Zero, newZoom)
    }

    private fun transformTo(
        centroid: Offset,
        panChange: Offset,
        targetZoom: Float
    ) {
        val newZoom = targetZoom.coerceIn(MinZoom, MaxZoom)
        // Keeps the point of the device that was under the centroid under it, as it moves.
        val moved = centroid + panChange - (centroid - pan) * (newZoom / zoom)
        zoom = newZoom
        pan = clamped(moved)
    }

    fun panBy(delta: Offset) {
        pan = clamped(pan + delta)
    }

    // A device smaller than the space stays centred; a larger one can't be dragged off its edges.
    private fun clamped(offset: Offset): Offset = if (zoom <= 1f) {
        Offset(size.width * (1f - zoom) / 2f, size.height * (1f - zoom) / 2f)
    } else {
        Offset(offset.x.coerceIn(size.width * (1f - zoom), 0f), offset.y.coerceIn(size.height * (1f - zoom), 0f))
    }
}

// Pinching, or Ctrl or Cmd with the wheel (a trackpad's pinch on the web), zooms about the pointer.
// A drag, or the wheel, pans where the device doesn't use it; two fingers are always the viewport's.
internal fun Modifier.zoomAndPan(viewport: PreviewViewport): Modifier = this
    .pointerInput(viewport) { recognizePinch(viewport) }
    .pointerInput(viewport) { recognizeWheelZoom(viewport) }
    .pointerInput(viewport) { recognizeWheelPan(viewport) }
    .pointerInput(viewport) { recognizeDragPan(viewport) }

// Before the device sees them, so that two fingers on the device zoom it rather than scroll it.
private suspend fun PointerInputScope.recognizePinch(viewport: PreviewViewport) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    do {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        if (event.changes.count { it.pressed } >= 2) {
            viewport.transformBy(event.calculateCentroid(useCurrent = false), event.calculatePan(), event.calculateZoom())
            event.changes.forEach {
                if (it.positionChange() != Offset.Zero) {
                    it.consume()
                }
            }
        }
    } while (event.changes.any { it.pressed })
}

private suspend fun PointerInputScope.recognizeWheelZoom(viewport: PreviewViewport) = awaitPointerEventScope {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val isZooming = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
        if (event.type == PointerEventType.Scroll && isZooming) {
            val change = event.changes.first()
            val step = exp(-change.scrollDelta.y * WheelZoomPerUnit).coerceIn(MinWheelZoomStep, MaxWheelZoomStep)
            viewport.transformBy(change.position, Offset.Zero, step)
            change.consume()
        }
    }
}

private suspend fun PointerInputScope.recognizeWheelPan(viewport: PreviewViewport) = awaitPointerEventScope {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.first()
        if (event.type == PointerEventType.Scroll && !change.isConsumed && viewport.canPan) {
            viewport.panBy(-change.scrollDelta * WheelPanPixels)
            change.consume()
        }
    }
}

// After the device has had its turn, so only a drag it left alone pans.
private suspend fun PointerInputScope.recognizeDragPan(viewport: PreviewViewport) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = true)
    if (!viewport.canPan) {
        return@awaitEachGesture
    }
    val slop = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
        change.consume()
        viewport.panBy(overSlop)
    } ?: return@awaitEachGesture
    drag(slop.id) { change ->
        viewport.panBy(change.positionChange())
        change.consume()
    }
}
