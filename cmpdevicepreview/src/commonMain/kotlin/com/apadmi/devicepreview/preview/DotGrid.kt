package com.apadmi.devicepreview.preview

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

/** Fills the node with [background] and a grid of round dots, [spacing] apart and centred on it. */
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

/** The dots of a grid [spacing] apart covering [size], with one on its centre. */
internal fun dotGridPoints(size: Size, spacing: Float): List<Offset> {
    if (size.isEmpty() || spacing <= 0f) return emptyList()
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
