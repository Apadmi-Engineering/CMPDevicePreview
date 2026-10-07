package com.kappmaker.devicepreview.devices

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import com.kappmaker.devicepreview.core.DeviceInsets
import com.kappmaker.devicepreview.core.DeviceKind
import com.kappmaker.devicepreview.core.DeviceType
import com.kappmaker.devicepreview.frame.DeviceFramePainter
import com.kappmaker.devicepreview.frame.SystemUiPainter
import com.kappmaker.devicepreview.svg.SvgDrawing
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class DevicePresetsTest {

    @Test
    fun catalogHoldsEverySpec() {
        assertEquals(33, DevicePresets.all.size)
        assertEquals(DevicePresets.all.size, DevicePresets.all.map { it.id }.toSet().size)
        assertSame(DevicePresets.iPhone16Pro, DevicePresets.byId("apple-iphone-16-pro"))
        assertNull(DevicePresets.byId("nokia-3310"))
    }

    @Test
    fun specValuesAreCarriedOver() {
        val iPhone = DevicePresets.iPhone16Pro
        assertEquals("iPhone 16 Pro", iPhone.name)
        assertEquals(DeviceType.IOS, iPhone.type)
        assertEquals(DeviceKind.PHONE, iPhone.kind)
        assertEquals(2024, iPhone.year)
        assertEquals(Size(402f, 874f), iPhone.portraitSize)
        assertEquals(1206f, iPhone.screenSize.width)
        assertEquals(2622f, iPhone.screenSize.height)
        assertEquals(3f, iPhone.screenSize.density)
        assertEquals(DeviceInsets(top = 62f, bottom = 34f), iPhone.portraitSafeArea)
        assertEquals(DeviceInsets(left = 62f, right = 62f, bottom = 20f), iPhone.landscapeSafeArea)

        assertEquals(DeviceType.ANDROID, DevicePresets.galaxyZFold8.type)
        assertEquals(DeviceKind.FOLDABLE, DevicePresets.galaxyZFold8.kind)
        assertEquals(2.625f, DevicePresets.pixel9.devicePixelRatio)
        assertEquals(DeviceType.OTHER, DevicePresets.largeDesktopWindow.type)
        assertEquals(DeviceInsets.Zero, DevicePresets.largeDesktopWindow.portraitSafeArea)
    }

    @Test
    fun everyFrameParsesAndWrapsItsScreen() {
        for (preset in DevicePresets.all) {
            val frame = assertNotNull(preset.frame, preset.id)
            val painter = DeviceFramePainter(frame)

            val body = assertNotNull(painter.body, "${preset.id}: body artwork")
            assertTrue(!body.isEmpty, "${preset.id}: body artwork is empty")
            assertEquals(frame.size, body.size, "${preset.id}: body view box")

            // The body surrounds the screen.
            val bodyBounds = frame.bodyBounds(preset.portraitSize, isPortrait = true)
            assertTrue(bodyBounds.left <= 0f && bodyBounds.top <= 0f, "${preset.id}: body starts after the screen")
            assertTrue(
                bodyBounds.right >= preset.portraitSize.width && bodyBounds.bottom >= preset.portraitSize.height,
                "${preset.id}: body ends before the screen",
            )

            // The screen outline covers the screen, in portrait and in landscape.
            val portrait = assertNotNull(painter.screenClip(preset.portraitSize, isPortrait = true), preset.id)
            assertRoughly(Rect(0f, 0f, preset.portraitSize.width, preset.portraitSize.height), portrait.getBounds(), preset.id)
            val landscapeSize = Size(preset.portraitSize.height, preset.portraitSize.width)
            val landscape = assertNotNull(painter.screenClip(landscapeSize, isPortrait = false), preset.id)
            assertRoughly(Rect(0f, 0f, landscapeSize.width, landscapeSize.height), landscape.getBounds(), preset.id)
        }
    }

    @Test
    fun systemUiAndKeyboardsAreCarriedOver() {
        val iPhone = DevicePresets.iPhone16Pro
        assertEquals(335f, iPhone.portraitKeyboardHeight)
        assertEquals(208f, iPhone.landscapeKeyboardHeight)
        assertEquals(30f, iPhone.systemUi?.statusBar?.inset)
        assertEquals(8f, iPhone.systemUi?.navigationBar?.bottomInset)

        val pixel = DevicePresets.pixel9
        assertEquals(336f, pixel.portraitKeyboardHeight)
        assertEquals(252f, pixel.landscapeKeyboardHeight)
        assertEquals(18f, pixel.systemUi?.statusBar?.inset)
        assertEquals(9f, pixel.systemUi?.navigationBar?.bottomInset)

        // A home-button iPhone has a status bar and no gesture pill.
        val se = assertNotNull(DevicePresets.iPhoneSe3.systemUi)
        assertNotNull(se.statusBar)
        assertNull(se.navigationBar)

        val desktop = DevicePresets.largeDesktopWindow
        assertNull(desktop.systemUi)
        assertNull(desktop.portraitKeyboardHeight)
        assertNull(desktop.landscapeKeyboardHeight)
        assertEquals(31, DevicePresets.all.count { it.systemUi != null })
    }

    @Test
    fun everySystemUiParsesAndFitsItsBars() {
        for (preset in DevicePresets.all) {
            val systemUi = preset.systemUi ?: continue
            val bars = listOfNotNull(systemUi.statusBar, systemUi.navigationBar)
            for (artwork in bars.flatMap { listOf(it.leading, it.center, it.trailing) }.filter { it.isNotEmpty() }) {
                val drawing = SvgDrawing.parse(artwork)
                assertTrue(!drawing.isEmpty && !drawing.size.isEmpty(), "${preset.id}: empty system UI artwork")
            }
            for (isPortrait in listOf(true, false)) {
                val screen = if (isPortrait) preset.portraitSize else Size(preset.portraitSize.height, preset.portraitSize.width)
                val safeArea = if (isPortrait) preset.portraitSafeArea else preset.landscapeSafeArea
                for (placed in SystemUiPainter(systemUi).layout(screen, safeArea, LayoutDirection.Ltr)) {
                    val bounds = placed.bounds
                    val inStatusBar = bounds.top < safeArea.top
                    val bar = if (inStatusBar) 0f..safeArea.top else (screen.height - safeArea.bottom)..screen.height
                    assertTrue(
                        bounds.left >= 0f && bounds.right <= screen.width &&
                            bounds.top >= bar.start && bounds.bottom <= bar.endInclusive + 0.01f,
                        "${preset.id} (${if (isPortrait) "portrait" else "landscape"}): $bounds outside its bar",
                    )
                }
            }
        }
    }

    private fun assertRoughly(expected: Rect, actual: Rect, message: String) {
        val tolerance = 1f
        if (abs(expected.left - actual.left) > tolerance || abs(expected.top - actual.top) > tolerance ||
            abs(expected.right - actual.right) > tolerance || abs(expected.bottom - actual.bottom) > tolerance
        ) {
            fail("$message: expected screen outline bounds $expected, got $actual")
        }
    }
}
