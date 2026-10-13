package com.apadmi.cmpdevicepreview.frame

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

/** Ported from flutter_device_preview's "SystemUiPainter layout" tests. */
class SystemUiPainterTest {
    private val screen = Size(400f, 800f)
    private val safeArea = DeviceInsets(top = 40f, bottom = 30f)
    private val systemUi = DeviceSystemUi(
        statusBar = SystemUiBar(leading = block(20, 10), trailing = block(30, 10), inset = 12f),
        navigationBar = SystemUiBar(center = block(100, 4), bottomInset = 6f),
    )

    private fun block(width: Int, height: Int) =
        """<svg viewBox="0 0 $width $height"><rect width="$width" height="$height" fill="currentColor"/></svg>"""

    private fun layout(
        systemUi: DeviceSystemUi = this.systemUi,
        safeArea: DeviceInsets = this.safeArea,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ): List<Rect> = SystemUiPainter(systemUi).layout(screen, safeArea, layoutDirection).map { it.bounds }

    @Test
    fun anchorsArtworkToTheEdgesAndCentersItInTheSafeArea() {
        assertEquals(
            listOf(Rect(12f, 15f, 32f, 25f), Rect(358f, 15f, 388f, 25f), Rect(150f, 790f, 250f, 794f)),
            layout(),
        )
    }

    @Test
    fun skipsABarWhoseSafeAreaIsZero() {
        assertEquals(listOf(Rect(150f, 790f, 250f, 794f)), layout(safeArea = DeviceInsets(bottom = 30f)))
        assertEquals(emptyList(), layout(safeArea = DeviceInsets.Zero))
    }

    @Test
    fun withoutABottomInsetTheArtworkIsCenteredVertically() {
        val pill = DeviceSystemUi(navigationBar = SystemUiBar(center = block(100, 4)))
        assertEquals(listOf(Rect(150f, 783f, 250f, 787f)), layout(systemUi = pill))
    }

    @Test
    fun leadingAndTrailingSwapUnderARightToLeftDirection() {
        val (leading, trailing) = layout(layoutDirection = LayoutDirection.Rtl)
        // The 30-wide trailing artwork is now on the left.
        assertEquals(Rect(12f, 15f, 42f, 25f), leading)
        assertEquals(Rect(368f, 15f, 388f, 25f), trailing)
    }

    @Test
    fun artworkTallerThanItsBarStartsAtTheBarTop() {
        val tall = DeviceSystemUi(statusBar = SystemUiBar(leading = block(20, 50)))
        assertEquals(0f, layout(systemUi = tall).single().top)
    }

    @Test
    fun malformedArtworkIsSkipped() {
        val painter = SystemUiPainter(DeviceSystemUi(statusBar = SystemUiBar(leading = "not svg at all")))
        assertEquals(emptyList(), painter.layout(screen, safeArea, LayoutDirection.Ltr))
        // Parsed once; a second layout is still empty, not an error.
        assertEquals(emptyList(), painter.layout(screen, safeArea, LayoutDirection.Ltr))
    }

    @Test
    fun tintsArtworkThatFollowsCurrentColor() {
        for (tint in listOf(SystemUiColors.DarkIcons, SystemUiColors.LightIcons)) {
            val image = ImageBitmap(400, 800)
            paintSystemUi(Canvas(image), SystemUiPainter(systemUi).layout(screen, safeArea, LayoutDirection.Ltr), tint)
            val pixels = image.toPixelMap()
            assertEquals(tint, pixels[20, 20], "inside the leading block")
            assertEquals(tint, pixels[200, 792], "inside the pill")
            assertEquals(Color.Transparent, pixels[5, 20], "inside the inset")
        }
    }

    @Test
    fun iconsContrastWithTheTheme() {
        assertEquals(SystemUiColors.LightIcons, SystemUiColors.icons(isDarkMode = true))
        assertEquals(SystemUiColors.DarkIcons, SystemUiColors.icons(isDarkMode = false))
    }
}
