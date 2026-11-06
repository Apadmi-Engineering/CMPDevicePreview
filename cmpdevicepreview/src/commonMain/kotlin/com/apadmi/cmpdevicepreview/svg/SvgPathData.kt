package com.apadmi.cmpdevicepreview.svg

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.graphics.PathSegment
import kotlin.math.sqrt

// Two halvings keep a quarter circle within a five-thousandth of its radius.
private const val ConicHalvings = 2

internal fun Path.toCssClipPath(): String {
    val fillRule = if (fillType == PathFillType.EvenOdd) "evenodd" else "nonzero"
    return "path($fillRule, \"${toSvgPathData()}\")"
}

// Compose's own toSvg() drops conics, which Skia keeps for rounded corners.
internal fun Path.toSvgPathData(): String {
    val iterator = iterator()
    return buildString { appendSegments(iterator) }
}

private fun StringBuilder.appendSegments(iterator: PathIterator) {
    val points = FloatArray(8)
    while (iterator.hasNext()) {
        when (iterator.next(points)) {
            PathSegment.Type.Move -> appendCommand('M', points, from = 0, count = 1)
            PathSegment.Type.Line -> appendCommand('L', points, from = 1, count = 1)
            PathSegment.Type.Quadratic -> appendCommand('Q', points, from = 1, count = 2)
            PathSegment.Type.Conic -> appendConic(
                start = Offset(points[0], points[1]),
                control = Offset(points[2], points[3]),
                end = Offset(points[4], points[5]),
                weight = points[6],
                halvings = ConicHalvings,
            )
            PathSegment.Type.Cubic -> appendCommand('C', points, from = 1, count = 3)
            PathSegment.Type.Close -> append('Z')
            PathSegment.Type.Done -> Unit
        }
    }
}

private fun StringBuilder.appendCommand(
    command: Char,
    points: FloatArray,
    from: Int,
    count: Int
) {
    append(command)
    for (point in from until from + count) {
        if (point > from) {
            append(' ')
        }
        appendPoint(points[point * 2], points[point * 2 + 1])
    }
}

private fun StringBuilder.appendPoint(x: Float, y: Float) {
    append(x)
    append(' ')
    append(y)
}

// Halves the conic as Skia's SkConic::chop does, until it's close enough to draw as quadratics.
private fun StringBuilder.appendConic(
    start: Offset,
    control: Offset,
    end: Offset,
    weight: Float,
    halvings: Int
) {
    if (halvings == 0) {
        append('Q')
        appendPoint(control.x, control.y)
        append(' ')
        appendPoint(end.x, end.y)
        return
    }
    val scale = 1f / (1f + weight)
    val weighted = control * weight
    val middle = (start + weighted * 2f + end) * (scale / 2f)
    val halfWeight = sqrt(0.5f + weight / 2f)
    appendConic(start, (start + weighted) * scale, middle, halfWeight, halvings - 1)
    appendConic(middle, (weighted + end) * scale, end, halfWeight, halvings - 1)
}
