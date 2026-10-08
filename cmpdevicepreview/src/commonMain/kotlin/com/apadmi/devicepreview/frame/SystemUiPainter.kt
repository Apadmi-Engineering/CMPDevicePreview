package com.apadmi.devicepreview.frame

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import com.apadmi.devicepreview.core.DeviceInsets
import com.apadmi.devicepreview.core.DeviceSystemUi
import com.apadmi.devicepreview.core.SystemUiBar
import com.apadmi.devicepreview.svg.SvgDrawing
import kotlin.math.max
import kotlin.math.min

/**
 * Parses a [DeviceSystemUi] once and paints it over the content.
 *
 * Each bar fills the safe area on its side of the screen; artwork is drawn at its natural (view
 * box) size, anchored to the leading edge, the center, or the trailing edge. Nothing is stretched,
 * so one description covers every screen width and both orientations.
 *
 * Malformed artwork is failure-isolated like device bodies: reported once, then skipped.
 */
internal class SystemUiPainter(val systemUi: DeviceSystemUi) {

    private val parsed = mutableMapOf<String, SvgDrawing?>()

    /**
     * Where each piece of artwork lands on a screen of [screenSize] whose safe area is [safeArea],
     * in paint order. Bars whose safe area is zero are skipped.
     */
    fun layout(screenSize: Size, safeArea: DeviceInsets, layoutDirection: LayoutDirection): List<PlacedArtwork> {
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

    /**
     * Paints the bars of a screen of [screenSize] whose safe area is [safeArea], tinting artwork
     * that follows `currentColor` with [tint].
     *
     * The canvas origin must be the screen's top-left corner, in logical pixels.
     */
    fun paint(canvas: Canvas, screenSize: Size, safeArea: DeviceInsets, tint: Color, layoutDirection: LayoutDirection) {
        for (artwork in layout(screenSize, safeArea, layoutDirection)) {
            artwork.drawing.paintInto(canvas, artwork.bounds, currentColor = tint)
        }
    }

    private fun placeBar(bar: SystemUiBar, region: Rect, layoutDirection: LayoutDirection, into: MutableList<PlacedArtwork>) {
        val rtl = layoutDirection == LayoutDirection.Rtl
        place(if (rtl) bar.trailing else bar.leading, region, bar, Anchor.Start)?.let(into::add)
        place(bar.center, region, bar, Anchor.Center)?.let(into::add)
        place(if (rtl) bar.leading else bar.trailing, region, bar, Anchor.End)?.let(into::add)
    }

    private fun place(source: String, region: Rect, bar: SystemUiBar, anchor: Anchor): PlacedArtwork? {
        if (source.isEmpty()) return null
        val drawing = drawing(source) ?: return null
        if (drawing.isEmpty || drawing.size.isEmpty()) return null
        val size = drawing.size
        val left = when (anchor) {
            Anchor.Start -> region.left + bar.inset
            Anchor.Center -> region.center.x - size.width / 2
            Anchor.End -> region.right - bar.inset - size.width
        }
        val top = bar.bottomInset?.let { region.bottom - it - size.height } ?: (region.center.y - size.height / 2)
        // Never let artwork spill outside its own bar.
        val clampedTop = max(region.top, min(top, region.bottom - size.height))
        return PlacedArtwork(drawing, Rect(left, clampedTop, left + size.width, clampedTop + size.height))
    }

    private fun drawing(source: String): SvgDrawing? {
        // Cache failures too, so malformed artwork is reported once.
        if (source in parsed) return parsed[source]
        return reportingParseFailure("system UI artwork") { SvgDrawing.parse(source) }.also { parsed[source] = it }
    }

    private enum class Anchor { Start, Center, End }
}

/** A piece of system UI artwork and where it is drawn, in screen coordinates. */
internal class PlacedArtwork(val drawing: SvgDrawing, val bounds: Rect)

/** The tints system UI artwork is painted with. */
internal object SystemUiColors {
    val LightIcons: Color = Color(0xFFFFFFFF)
    val DarkIcons: Color = Color(0xFF16181C)

    /** Icons that contrast with the simulated theme: light icons in dark mode, dark ones otherwise. */
    fun icons(isDarkMode: Boolean): Color = if (isDarkMode) LightIcons else DarkIcons
}
