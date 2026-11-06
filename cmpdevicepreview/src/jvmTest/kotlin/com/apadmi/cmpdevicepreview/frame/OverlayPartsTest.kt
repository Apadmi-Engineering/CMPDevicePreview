package com.apadmi.cmpdevicepreview.frame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.LayoutDirection
import com.apadmi.cmpdevicepreview.core.DeviceInsets
import com.apadmi.cmpdevicepreview.core.DeviceSystemUi
import com.apadmi.cmpdevicepreview.core.SystemUiBar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverlayPartsTest {
    private val screen = Size(400f, 800f)
    private val safeArea = DeviceInsets(top = 40f, bottom = 30f)
    private val systemUi = SystemUiPainter(
        DeviceSystemUi(
            statusBar = SystemUiBar(leading = block(20, 10), trailing = block(30, 10), inset = 12f),
            navigationBar = SystemUiBar(center = block(100, 4), bottomInset = 6f),
        )
    )

    private fun block(width: Int, height: Int) =
        """<svg viewBox="0 0 $width $height"><rect width="$width" height="$height" fill="currentColor"/></svg>"""

    private fun overlay(keyboardInset: Float = 300f) =
        overlayParts(systemUi, keyboardInset, screen, safeArea, LayoutDirection.Ltr, isDarkMode = false)

    @Test
    fun theKeyboardIsPaintedUnderTheBars() {
        assertEquals(
            listOf(Rect(0f, 500f, 400f, 800f), Rect(0f, 0f, 400f, 40f), Rect(0f, 770f, 400f, 800f)),
            overlay().map { it.region },
        )
    }

    @Test
    fun withNothingShownThereIsNothingToDraw() {
        assertTrue(overlayParts(null, keyboardInset = 0f, screen, safeArea, LayoutDirection.Ltr, isDarkMode = false).isEmpty())
    }

    @Test
    fun eachPartPaintsOnlyInsideItsRegion() {
        for (part in overlay()) {
            // A pixel's worth of room for anti-aliasing at the edges.
            val outside = paintedPixels(part).filterNot { part.region.inflate(1f).contains(it) }
            assertTrue(outside.isEmpty(), "${part.region} is painted outside, at ${outside.take(3)}")
        }
    }

    // The centres of the pixels the part paints on a screen-sized image.
    private fun paintedPixels(part: OverlayPart): List<Offset> {
        val image = ImageBitmap(screen.width.toInt(), screen.height.toInt())
        part.paint(Canvas(image))
        val pixels = image.toPixelMap()
        return (0 until pixels.width).flatMap { x ->
            (0 until pixels.height).filter { y -> pixels[x, y] != Color.Transparent }.map { y -> Offset(x + 0.5f, y + 0.5f) }
        }
    }
}
