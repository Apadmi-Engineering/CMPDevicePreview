package com.apadmi.cmpdevicepreview.devices

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import com.apadmi.cmpdevicepreview.assertRectEquals
import com.apadmi.cmpdevicepreview.core.DeviceInsets
import com.apadmi.cmpdevicepreview.core.DeviceKind
import com.apadmi.cmpdevicepreview.core.DeviceType
import com.apadmi.cmpdevicepreview.frame.DeviceFramePainter
import com.apadmi.cmpdevicepreview.frame.SystemUiPainter
import com.apadmi.cmpdevicepreview.svg.SvgDrawing
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DevicePresetsTest {

    @Test
    fun catalogHoldsEverySpec() {
        val specIds = File("device_specs").listFiles { file -> file.extension == "json" }.orEmpty().map { it.nameWithoutExtension }
        assertEquals(specIds.sorted(), DevicePresets.all.map { it.id })
    }

    @Test
    fun presetsAreFoundById() {
        assertSame(DevicePresets.iPhone16Pro, DevicePresets.byId("apple-iphone-16-pro"))
        assertNull(DevicePresets.byId("nokia-3310"))
    }

    @Test
    fun anIPhoneSpecIsCarriedOver() {
        val iPhone = DevicePresets.iPhone16Pro
        assertEquals("iPhone 16 Pro", iPhone.name)
        assertEquals(DeviceType.IOS, iPhone.type)
        assertEquals(DeviceKind.PHONE, iPhone.kind)
        assertEquals(2024, iPhone.year)
        assertEquals(Size(402f, 874f), iPhone.portraitSize)
        assertEquals(3f, iPhone.devicePixelRatio)
        assertEquals(DeviceInsets(top = 62f, bottom = 34f), iPhone.portraitSafeArea)
        assertEquals(DeviceInsets(left = 62f, right = 62f, bottom = 20f), iPhone.landscapeSafeArea)
    }

    @Test
    fun platformsKindsAndPixelRatiosAreCarriedOver() {
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
            assertFalse(body.isEmpty, "${preset.id}: body artwork is empty")
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
            assertRectEquals(Rect(Offset.Zero, preset.portraitSize), portrait.getBounds(), tolerance = 1f, preset.id)
            val landscapeSize = Size(preset.portraitSize.height, preset.portraitSize.width)
            val landscape = assertNotNull(painter.screenClip(landscapeSize, isPortrait = false), preset.id)
            assertRectEquals(Rect(Offset.Zero, landscapeSize), landscape.getBounds(), tolerance = 1f, preset.id)
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
    }

    @Test
    fun everyDeviceButADesktopWindowHasSystemUi() {
        for (preset in DevicePresets.all) {
            assertEquals(preset.kind != DeviceKind.DESKTOP, preset.systemUi != null, preset.id)
        }
    }

    @Test
    fun everySystemUiParsesAndFitsItsBars() {
        for (preset in DevicePresets.all) {
            val systemUi = preset.systemUi ?: continue
            val bars = listOfNotNull(systemUi.statusBar, systemUi.navigationBar)
            for (artwork in bars.flatMap { listOf(it.leading, it.center, it.trailing) }.filter { it.isNotEmpty() }) {
                val drawing = SvgDrawing.parse(artwork)
                assertFalse(drawing.isEmpty || drawing.size.isEmpty(), "${preset.id}: empty system UI artwork")
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
}
