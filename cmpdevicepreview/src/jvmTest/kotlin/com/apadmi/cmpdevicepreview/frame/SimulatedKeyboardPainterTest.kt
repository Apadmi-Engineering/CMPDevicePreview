package com.apadmi.cmpdevicepreview.frame

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SimulatedKeyboardPainterTest {

    private val screen = Size(400f, 800f)

    @Test
    fun theBandCoversTheBottomOfTheScreen() {
        assertEquals(Rect(0f, 500f, 400f, 800f), keyboardBand(screen, 300f))
        // A keyboard taller than the screen is clamped to it.
        assertEquals(Rect(0f, 0f, 400f, 200f), keyboardBand(Size(400f, 200f), 900f))
        assertNull(keyboardBand(screen, 0f))
        assertNull(keyboardBand(screen, -1f))
    }

    @Test
    fun theMarkScalesWithTheBandAndDisappearsWhenTooSmall() {
        assertEquals(56f, keyboardMarkSize(Rect(0f, 500f, 400f, 800f)))
        assertEquals(24f, keyboardMarkSize(Rect(0f, 720f, 400f, 800f)))
        assertNull(keyboardMarkSize(Rect(0f, 760f, 400f, 800f)))
    }

    @Test
    fun paintsATranslucentHatchedBandOverTheContent() {
        val pixels = paintOverWhite(height = 300f)
        assertEquals(Color.White, pixels[200, 499], "above the band")
        assertEquals(Color.White, pixels[0, 500], "outside the rounded corner")

        // The band is 80% near-black: what it covers still shows a little.
        val band = pixels[10, 700]
        assertTrue(band.red in 0.24f..0.30f, "band at (10, 700) was $band")
        // A hatch stripe crosses (15, 700); the hairline runs along the band's top edge.
        assertTrue(pixels[15, 700].red > band.red, "stripe")
        assertTrue(pixels[100, 500].red > pixels[100, 503].red, "hairline")
    }

    @Test
    fun nothingIsPaintedWithoutAKeyboard() {
        val pixels = paintOverWhite(height = 0f)
        assertEquals(Color.White, pixels[10, 700])
    }

    private fun paintOverWhite(height: Float): PixelMap {
        val image = ImageBitmap(400, 800)
        val canvas = Canvas(image)
        canvas.drawRect(Rect(0f, 0f, 400f, 800f), Paint().apply { color = Color.White })
        paintSimulatedKeyboard(canvas, screen, height)
        return image.toPixelMap()
    }
}
