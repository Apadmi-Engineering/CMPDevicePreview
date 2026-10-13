package com.apadmi.cmpdevicepreview.frame

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import kotlin.math.min

/** The radius of the keyboard band's top corners, in logical pixels. */
private const val KeyboardBandRadius: Float = 8f

/** The pitch of the band's hatching, in logical pixels. */
private const val HatchPitch: Float = 12f

/** The keyboard band's paints, made once, on first use. */
private object KeyboardPaints {
    private val MarkInk = Color(0x40FFFFFF)
    val band = Paint().apply { color = Color(0xCC17171A) }
    val hairline = Paint().apply { color = Color(0x1FFFFFFF) }
    val stripe = Paint().apply {
        style = PaintingStyle.Stroke
        strokeWidth = 1f
        color = Color(0x14FFFFFF)
    }
    val markStroke = Paint().apply {
        style = PaintingStyle.Stroke
        strokeWidth = 1f
        strokeCap = StrokeCap.Round
        strokeJoin = StrokeJoin.Round
        color = MarkInk
    }
    val markFill = Paint().apply { color = MarkInk }
}

/**
 * The band a keyboard of [height] covers at the bottom of a screen of [screenSize], or null when
 * nothing is covered. A keyboard taller than the screen is clamped to it.
 *
 * @param screenSize The size of the screen the keyboard is raised on, in dp.
 * @param height The height the keyboard covers, in dp.
 * @return The covered band in the screen's coordinates, or null when nothing is covered.
 */
internal fun keyboardBand(screenSize: Size, height: Float): Rect? {
    if (height <= 0f || screenSize.isEmpty()) {
        return null
    }
    val covered = min(height, screenSize.height)
    return Rect(0f, screenSize.height - covered, screenSize.width, screenSize.height)
}

/**
 * The size of the keyboard mark in [band], or null when the band is too small for a legible one.
 *
 * Sized off the band so it stays proportionate from a landscape phone strip to an iPad's
 * half-screen keyboard.
 *
 * @param band The band the keyboard covers, in dp.
 * @return The side of the square keyboard mark in dp, or null when the band is too small for one.
 */
internal fun keyboardMarkSize(band: Rect): Float? =
    minOf(band.height * 0.30f, band.width * 0.20f, 56f).takeIf { it >= 16f }

/**
 * Paints the band a software keyboard of [height] covers at the bottom of the screen: translucent
 * and hatched, with a keyboard mark, so the content under it stays legible. The canvas origin must
 * be the screen's top-left corner, in dp.
 *
 * @param canvas The canvas to paint on.
 * @param screenSize The size of the screen the keyboard is raised on, in dp.
 * @param height The height the keyboard covers, in dp; nothing is painted when it is zero or less.
 */
internal fun paintSimulatedKeyboard(
    canvas: Canvas,
    screenSize: Size,
    height: Float
) {
    val band = keyboardBand(screenSize, height) ?: return
    // Rounded where it meets the content, square where it meets the screen edge.
    val shape = Path().apply {
        addRoundRect(
            RoundRect(band, topLeft = CornerRadius(KeyboardBandRadius), topRight = CornerRadius(KeyboardBandRadius))
        )
    }
    canvas.drawPath(shape, KeyboardPaints.band)
    // The hatching and the hairline follow the rounded corners instead of cutting across them.
    canvas.save()
    canvas.clipPath(shape)
    paintHatch(canvas, band)
    canvas.drawRect(Rect(band.left, band.top, band.right, band.top + 1f), KeyboardPaints.hairline)
    canvas.restore()

    val markSize = keyboardMarkSize(band) ?: return
    canvas.save()
    canvas.translate(band.center.x - markSize / 2, band.center.y - markSize / 2)
    canvas.scale(markSize / 16f)
    paintKeyboardMark(canvas)
    canvas.restore()
}

/** Fills [band] with 45° hatching at a fixed pitch, clipped to it. */
private fun paintHatch(canvas: Canvas, band: Rect) {
    canvas.save()
    canvas.clipRect(band)
    // Start a full band-height to the left so the first slanted stripe still crosses the left edge.
    var x = band.left - band.height
    while (x < band.right) {
        canvas.drawLine(Offset(x, band.bottom), Offset(x + band.height, band.top), KeyboardPaints.stripe)
        x += HatchPitch
    }
    canvas.restore()
}

/** Draws the keyboard mark on a 16×16 grid at the canvas origin: an outline, eight keys and a space bar. */
private fun paintKeyboardMark(canvas: Canvas) {
    val stroke = KeyboardPaints.markStroke
    val fill = KeyboardPaints.markFill
    canvas.drawRoundRect(1.25f, 3.75f, 14.75f, 12.25f, 1.75f, 1.75f, stroke)
    for (y in listOf(6.3f, 8.6f)) {
        for (x in listOf(4.0f, 6.5f, 9.0f, 11.5f)) {
            canvas.drawCircle(Offset(x, y), 0.55f, fill)
        }
    }
    canvas.drawLine(Offset(5.5f, 10.6f), Offset(10.5f, 10.6f), stroke)
}
