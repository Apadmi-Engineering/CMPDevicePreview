package com.apadmi.cmpdevicepreview.frame

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import com.apadmi.cmpdevicepreview.core.DeviceInsets
import com.apadmi.cmpdevicepreview.core.DeviceSystemUi
import com.apadmi.cmpdevicepreview.core.SystemUiBar
import com.apadmi.cmpdevicepreview.svg.SvgDrawing
import kotlin.math.max
import kotlin.math.min

/**
 * Lays out a [DeviceSystemUi]'s bars, parsing each piece of artwork once. Malformed artwork is
 * skipped.
 *
 * @property systemUi The status bar and navigation bar artwork to lay out.
 */
internal class SystemUiPainter(val systemUi: DeviceSystemUi) {
    private val parsed = mutableMapOf<String, SvgDrawing?>()

    /**
     * Where each piece of artwork lands on a screen of [screenSize] whose safe area is [safeArea],
     * in paint order. Bars whose safe area is zero are skipped.
     *
     * @param screenSize The size of the screen, in dp.
     * @param safeArea The screen's safe area, whose top and bottom give the heights of the status
     * bar and navigation bar.
     * @param layoutDirection The device's layout direction; leading and trailing artwork swap sides
     * in right-to-left.
     * @return The placed artwork, in paint order.
     */
    fun layout(
        screenSize: Size,
        safeArea: DeviceInsets,
        layoutDirection: LayoutDirection
    ): List<PlacedArtwork> {
        val placed = mutableListOf<PlacedArtwork>()
        val statusBar = systemUi.statusBar
        if (statusBar != null && safeArea.top > 0f) {
            placeBar(statusBar, Rect(0f, 0f, screenSize.width, safeArea.top), layoutDirection, placed)
        }
        val navigationBar = systemUi.navigationBar
        if (navigationBar != null && safeArea.bottom > 0f) {
            val region = Rect(0f, screenSize.height - safeArea.bottom, screenSize.width, screenSize.height)
            placeBar(navigationBar, region, layoutDirection, placed)
        }
        return placed
    }

    private fun placeBar(
        bar: SystemUiBar,
        region: Rect,
        layoutDirection: LayoutDirection,
        into: MutableList<PlacedArtwork>
    ) {
        val rtl = layoutDirection == LayoutDirection.Rtl
        place(if (rtl) bar.trailing else bar.leading, region, bar, Anchor.Start)?.let(into::add)
        place(bar.center, region, bar, Anchor.Center)?.let(into::add)
        place(if (rtl) bar.leading else bar.trailing, region, bar, Anchor.End)?.let(into::add)
    }

    private fun place(
        source: String,
        region: Rect,
        bar: SystemUiBar,
        anchor: Anchor
    ): PlacedArtwork? {
        if (source.isEmpty()) {
            return null
        }
        val drawing = drawing(source) ?: return null
        if (drawing.isEmpty || drawing.size.isEmpty()) {
            return null
        }
        val size = drawing.size
        val left = when (anchor) {
            Anchor.Start -> region.left + bar.inset
            Anchor.Center -> region.center.x - size.width / 2
            Anchor.End -> region.right - bar.inset - size.width
        }
        val top = bar.bottomInset?.let { region.bottom - it - size.height } ?: (region.center.y - size.height / 2)
        // Keep artwork inside its bar; artwork taller than the bar starts at the bar's top.
        val clampedTop = max(region.top, min(top, region.bottom - size.height))
        return PlacedArtwork(drawing, Rect(left, clampedTop, left + size.width, clampedTop + size.height))
    }

    private fun drawing(source: String): SvgDrawing? {
        // Cache failures too, so malformed artwork is reported once.
        if (source in parsed) {
            return parsed[source]
        }
        return reportingParseFailure("system UI artwork") { SvgDrawing.parse(source) }.also { parsed[source] = it }
    }

    private enum class Anchor {
        Start, Center, End
    }
}

/**
 * A piece of system UI artwork and where it is drawn, in screen coordinates.
 *
 * @property drawing The parsed artwork.
 * @property bounds Where the artwork is drawn, in the screen's coordinates.
 */
internal class PlacedArtwork(val drawing: SvgDrawing, val bounds: Rect)

/** The tints system UI artwork is painted with. */
internal object SystemUiColors {
    val LightIcons: Color = Color(0xFFFFFFFF)
    val DarkIcons: Color = Color(0xFF16181C)

    /**
     * Icons that contrast with the simulated theme: light icons in dark mode, dark ones otherwise.
     *
     * @param isDarkMode Whether the simulated device is in dark mode.
     * @return The colour to tint the system UI artwork with.
     */
    fun icons(isDarkMode: Boolean): Color = if (isDarkMode) LightIcons else DarkIcons
}

/**
 * Paints system UI artwork laid out by [SystemUiPainter.layout], tinting what follows
 * `currentColor` with [tint]. The canvas origin must be the screen's top-left corner, in logical
 * pixels.
 *
 * @param canvas The canvas to paint on.
 * @param placed The artwork to paint, as laid out by [SystemUiPainter.layout].
 * @param tint The colour for shapes filled with `currentColor`.
 */
internal fun paintSystemUi(
    canvas: Canvas,
    placed: List<PlacedArtwork>,
    tint: Color
) {
    for (artwork in placed) {
        artwork.drawing.paintInto(canvas, artwork.bounds, currentColor = tint)
    }
}
