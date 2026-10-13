package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The backdrop behind the simulated device. */
internal object DotGridDefaults {
    val Background = Color(0xFFF2F2F4)
    val Dot = Color(0xFFC8C8D0)
    val DarkBackground = Color(0xFF18181C)
    val DarkDot = Color(0xFF2E2E36)
    val Spacing = 20.dp
    val DotSize = 2.dp
}

/**
 * Fills the node with [background] and a grid of round dots, [spacing] apart and centred on it.
 *
 * @param background The colour that fills the node.
 * @param dot The colour of the dots.
 * @param spacing The distance between neighbouring dots.
 * @param dotSize The diameter of a dot.
 * @return This modifier, followed by one that draws the background and the dots behind the content.
 */
internal fun Modifier.dotGrid(
    background: Color = DotGridDefaults.Background,
    dot: Color = DotGridDefaults.Dot,
    spacing: Dp = DotGridDefaults.Spacing,
    dotSize: Dp = DotGridDefaults.DotSize,
): Modifier = drawWithCache {
    val points = dotGridPoints(size, spacing.toPx())
    val strokeWidth = dotSize.toPx()
    onDrawBehind {
        drawRect(background)
        drawPoints(points, PointMode.Points, dot, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

/**
 * The dots of a grid [spacing] apart covering [size], with one on its centre.
 *
 * @param size The area the grid covers, in pixels.
 * @param spacing The distance between neighbouring dots, in pixels.
 * @return The position of each dot, in pixels from the top left of the area, row by row. Empty when
 * [size] is empty or [spacing] isn't positive.
 */
internal fun dotGridPoints(size: Size, spacing: Float): List<Offset> {
    if (size.isEmpty() || spacing <= 0f) {
        return emptyList()
    }
    val firstX = (size.width / 2f) % spacing
    val firstY = (size.height / 2f) % spacing
    val columns = ((size.width - firstX) / spacing).toInt() + 1
    val rows = ((size.height - firstY) / spacing).toInt() + 1
    return buildList(columns * rows) {
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                add(Offset(firstX + column * spacing, firstY + row * spacing))
            }
        }
    }
}
