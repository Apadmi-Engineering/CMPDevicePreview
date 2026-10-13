package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/** The smallest a freeform window can be on either side, in dp. */
internal const val MinFreeformSize = 120f

/** The largest a freeform window can be on either side, in dp. */
internal const val MaxFreeformSize = 4096f

/** The side of a handle's hit area, centred on a corner or an edge of the device. */
internal val ResizeHandleSize = 24.dp

private val HandleHighlight = PanelColors.Accent

/**
 * A place on the device that can be dragged to resize it: a corner or an edge midpoint. [x] and [y]
 * are each -1 (left, top), 0 (the middle) or 1 (right, bottom): the sides the handle moves.
 * @property x
 * @property y
 */
internal data class ResizeHandle(val x: Int, val y: Int) {
    val isCorner: Boolean get() = x != 0 && y != 0
    init {
        require(x in -1..1 && y in -1..1 && (x != 0 || y != 0)) { "Not a handle: ($x, $y)" }
    }

    companion object {
        val Corners = listOf(ResizeHandle(-1, -1), ResizeHandle(1, -1), ResizeHandle(1, 1), ResizeHandle(-1, 1))
        val Edges = listOf(ResizeHandle(0, -1), ResizeHandle(1, 0), ResizeHandle(0, 1), ResizeHandle(-1, 0))
    }
}

/**
 * A resize in progress: [handle] has been dragged [offset] pixels from where it was pressed. The
 * [scale] stays as it was when the drag began, so the dragged corner doesn't run away from the cursor.
 * @property handle
 * @property start
 * @property scale
 * @property offset
 */
internal data class ResizeDrag(
    val handle: ResizeHandle,
    val start: Size,
    val scale: Float,
    val offset: Offset = Offset.Zero,
)

/**
 * The handles on a device whose visible bounds measure [visibleSize] pixels, centred in the parent.
 * A freeform window has edge handles too, and its corners show even when the pointer isn't on them.
 *
 * @param visibleSize The size of the device's visible bounds, in pixels, which the handles sit on.
 * @param isFreeform Whether the device is a freeform window, which has edge handles as well.
 * @param active The handle being dragged, drawn highlighted, or null when none is.
 * @param onStart Called with the handle when a drag on it begins.
 * @param onDrag Called with how far the pointer is from where it was pressed, in pixels, each time
 * it moves during a drag.
 * @param onEnd Called when the pointer is released at the end of a drag.
 * @param onCancel Called when a drag ends without a release, because the gesture was cancelled.
 */
@Composable
internal fun BoxScope.ResizeHandles(
    visibleSize: IntSize,
    isFreeform: Boolean,
    active: ResizeHandle?,
    onStart: (ResizeHandle) -> Unit,
    onDrag: (Offset) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
) {
    val handles = if (isFreeform) ResizeHandle.Corners + ResizeHandle.Edges else ResizeHandle.Corners
    for (handle in handles) {
        ResizeHandleView(
            handle = handle,
            visibleSize = visibleSize,
            isAlwaysVisible = isFreeform && handle.isCorner,
            isActive = handle == active,
            onStart = onStart,
            onDrag = onDrag,
            onEnd = onEnd,
            onCancel = onCancel,
        )
    }
}

/**
 * A dashed outline of a freeform window of [size] dp at [scale] pixels per dp, centred on the
 * parent, with its size written in it: where the device lands when the handle is let go.
 *
 * @param size The size of the window, in dp.
 * @param scale The pixels per dp the outline is drawn at.
 */
@Composable
internal fun BoxScope.ResizeOutline(size: Size, scale: Float) {
    val density = LocalDensity.current
    Box(
        Modifier
            .align(Alignment.Center)
            .requiredSize(with(density) { DpSize((size.width * scale).toDp(), (size.height * scale).toDp()) })
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRect(HandleHighlight.copy(alpha = 0.10f))
                drawRect(
                    color = HandleHighlight,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(this.size.width - stroke, this.size.height - stroke),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            size.dimensionsLabel(),
            modifier = Modifier
                .drawBehind { drawRoundRect(HandleHighlight, cornerRadius = CornerRadius(6.dp.toPx())) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun BoxScope.ResizeHandleView(
    handle: ResizeHandle,
    visibleSize: IntSize,
    isAlwaysVisible: Boolean,
    isActive: Boolean,
    onStart: (ResizeHandle) -> Unit,
    onDrag: (Offset) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val isHovered by interactions.collectIsHoveredAsState()
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnEnd by rememberUpdatedState(onEnd)
    val currentOnCancel by rememberUpdatedState(onCancel)
    val isHighlighted = isHovered || isActive

    Box(
        Modifier
            .align(Alignment.Center)
            .offset { IntOffset(handle.x * visibleSize.width / 2, handle.y * visibleSize.height / 2) }
            .size(ResizeHandleSize)
            .hoverable(interactions)
            .pointerHoverIcon(resizeCursor(handle))
            .pointerInput(handle) {
                // Measured from where the pointer went down, not from where the slop was crossed, so
                // the corner follows the cursor exactly.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val crossed = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                        ?: return@awaitEachGesture
                    currentOnStart(handle)
                    currentOnDrag(crossed.position - down.position)
                    val isReleased = drag(crossed.id) { change ->
                        change.consume()
                        currentOnDrag(change.position - down.position)
                    }
                    if (isReleased) currentOnEnd() else currentOnCancel()
                }
            }
            .drawBehind {
                if (isHighlighted || isAlwaysVisible) {
                    drawHandle(handle, isHighlighted)
                }
            }
    )
}

private fun DrawScope.drawHandle(handle: ResizeHandle, isHighlighted: Boolean) {
    val color = if (isHighlighted) HandleHighlight else PanelColors.FrameOutline
    val width = (if (isHighlighted) 3.dp else 2.dp).toPx()
    val outward = 4.dp.toPx()
    val tip = center + Offset(handle.x * outward, handle.y * outward)
    if (handle.isCorner) {
        // A bracket hugging the corner from outside, along both sides.
        val arm = 10.dp.toPx()
        drawLine(color, tip, tip - Offset(handle.x * arm, 0f), width, StrokeCap.Round)
        drawLine(color, tip, tip - Offset(0f, handle.y * arm), width, StrokeCap.Round)
    } else {
        // A short bar along the edge.
        val half = Offset(abs(handle.y) * 8.dp.toPx(), abs(handle.x) * 8.dp.toPx())
        drawLine(color, tip - half, tip + half, width, StrokeCap.Round)
    }
}

/**
 * The screen size, in dp, after dragging [handle] by [drag] pixels at [scale] pixels per dp. The
 * device stays centred, so each side the handle moves grows by twice the drag. Sides are whole dp,
 * between [MinFreeformSize] and [max] (at most [MaxFreeformSize]), unless already larger.
 *
 * @param start The screen size, in dp, when the drag began.
 * @param handle The handle being dragged.
 * @param drag How far the handle has been dragged from where it was pressed, in pixels.
 * @param scale The pixels per dp the drag is measured at.
 * @param max The room there is for the device, in dp; a side with no limit is infinite.
 * @return The new screen size, in dp, with any side the handle doesn't move left as it was.
 */
internal fun resizedScreenSize(
    start: Size,
    handle: ResizeHandle,
    drag: Offset,
    scale: Float,
    max: Size
): Size = Size(
    width = resizedSide(start.width, handle.x, drag.x, scale, max.width),
    height = resizedSide(start.height, handle.y, drag.y, scale, max.height),
)

/**
 * The cursor shown over [handle].
 *
 * @param handle The handle the pointer is over.
 * @return The resize cursor that points along the handle's direction, or the default cursor on a
 * platform that has none.
 */
internal expect fun resizeCursor(handle: ResizeHandle): PointerIcon

/** One side of [resizedScreenSize]: [start] moved by [drag] pixels in [direction], or as it was when 0. */
private fun resizedSide(
    start: Float,
    direction: Int,
    drag: Float,
    scale: Float,
    max: Float
): Float {
    if (direction == 0) {
        return start
    }
    val upper = maxOf(MinFreeformSize, floor(minOf(max, MaxFreeformSize)), start)
    return (start + 2f * direction * drag / scale).roundToInt().toFloat().coerceIn(MinFreeformSize, upper)
}
