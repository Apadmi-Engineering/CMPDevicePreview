package com.kappmaker.devicepreview.frame

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.kappmaker.devicepreview.core.DeviceFrame
import com.kappmaker.devicepreview.devices.DevicePresets
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeviceFramePainterTest {

    private val iPhone = DevicePresets.iPhone16Pro
    private val frame = iPhone.frame!!
    private val portrait = iPhone.portraitSize
    private val landscape = Size(portrait.height, portrait.width)

    @Test
    fun bodyBoundsRotateAQuarterTurnInLandscape() {
        // 436×908 body, screen at (17, 17).
        assertEquals(Rect(-17f, -17f, 419f, 891f), frame.bodyBounds(portrait, isPortrait = true))
        assertEquals(Rect(-17f, -17f, 891f, 419f), frame.bodyBounds(landscape, isPortrait = false))
    }

    @Test
    fun paintsTheBodyAroundTheScreen() {
        val image = paintBody(isPortrait = true)
        assertEquals(436, image.width)
        assertEquals(908, image.height)
        val pixels = image.toPixelMap()
        // Outside the rounded body.
        assertEquals(Color.Transparent, pixels[0, 0])
        // The #2c2c2c band of the bezel, on the left edge.
        assertEquals(Color(0xFF2C2C2C), pixels[5, 454])
        // Behind the screen.
        assertEquals(Color.Black, pixels[218, 454])
    }

    @Test
    fun paintsTheBodyRotatedInLandscape() {
        val image = paintBody(isPortrait = false)
        assertEquals(908, image.width)
        assertEquals(436, image.height)
        // Body point (x, y) lands on (y, 436 − x): the left edge becomes the bottom one.
        assertEquals(Color(0xFF2C2C2C), image.toPixelMap()[454, 431])
    }

    @Test
    fun screenOutlinePunchesTheDynamicIsland() {
        val outline = DeviceScreenShape(DeviceFramePainter(frame).screenClip(portrait, isPortrait = true)!!, portrait)
            .createOutline(portrait, LayoutDirection.Ltr, Density(1f))
        val path = assertIs<Outline.Generic>(outline).path
        val image = ImageBitmap(portrait.width.roundToInt(), portrait.height.roundToInt())
        Canvas(image).drawPath(path, Paint().apply { color = Color.Black })
        val pixels = image.toPixelMap()
        assertEquals(Color.Transparent, pixels[0, 0], "rounded corner")
        assertEquals(Color.Transparent, pixels[201, 29], "Dynamic Island")
        assertEquals(Color.Black, pixels[201, 100], "screen")
    }

    @Test
    fun malformedArtworkIsSkipped() {
        val painter = DeviceFramePainter(DeviceFrame(size = Size(10f, 10f), body = "<svg><rect", screenPath = ""))
        assertNull(painter.body)
        assertNull(painter.screenPath)
        assertNotNull(DeviceFramePainter(frame).body)
    }

    /** Paints the iPhone's body the way DeviceFrameView does, at one pixel per logical pixel. */
    private fun paintBody(isPortrait: Boolean): ImageBitmap {
        val screen = if (isPortrait) portrait else landscape
        val bounds = frame.bodyBounds(screen, isPortrait)
        val image = ImageBitmap(bounds.width.roundToInt(), bounds.height.roundToInt())
        Canvas(image).apply {
            translate(-bounds.left, -bounds.top)
            DeviceFramePainter(frame).paintBody(this, screen, isPortrait)
        }
        return image
    }
}
