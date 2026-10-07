package com.kappmaker.devicepreview.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.unit.LayoutDirection
import com.kappmaker.devicepreview.core.Device
import com.kappmaker.devicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Renders [DeviceFrameView] without its body, in a scene exactly the size of the iPhone 16 Pro's
 * screen, so one scene pixel is one logical pixel of the device.
 */
class DeviceFrameViewTest {

    private val iPhone = DevicePresets.iPhone16Pro

    /** What the content sees, through Compose's own APIs. */
    private class Captured(
        var safeAreaBottom: Int = -1,
        var keyboardBottom: Int = -1,
        var statusBarTop: Int = -1,
        var isDark: Boolean? = null,
        var paddedHeight: Int = -1,
    )

    private fun render(
        device: Device = iPhone,
        isSystemUiVisible: Boolean = true,
        isKeyboardVisible: Boolean = false,
        isDarkMode: Boolean = false,
        background: Color = Color.White,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        captured: Captured = Captured(),
    ): PixelMap = renderComposeScene(402, 874) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            DeviceFrameView(
                device = device,
                isFrameVisible = false,
                isSystemUiVisible = isSystemUiVisible,
                isKeyboardVisible = isKeyboardVisible,
                isDarkMode = isDarkMode,
            ) {
                val density = LocalDensity.current
                captured.safeAreaBottom = WindowInsets.safeDrawing.getBottom(density)
                captured.keyboardBottom = WindowInsets.ime.getBottom(density)
                captured.statusBarTop = WindowInsets.statusBars.getTop(density)
                captured.isDark = isSystemInDarkTheme()
                Box(Modifier.fillMaxSize().background(background)) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding().onSizeChanged { captured.paddedHeight = it.height })
                }
            }
        }
    }.toComposeImageBitmap().toPixelMap()

    @Test
    fun aRaisedKeyboardJoinsTheSafeArea() {
        val raised = Captured().also { render(isKeyboardVisible = true, captured = it) }
        assertEquals(335, raised.safeAreaBottom)
        assertEquals(335, raised.keyboardBottom)

        val hidden = Captured().also { render(captured = it) }
        assertEquals(34, hidden.safeAreaBottom)
        assertEquals(0, hidden.keyboardBottom)

        // A desktop window has no software keyboard to raise.
        val desktop = Captured().also {
            render(device = DevicePresets.largeDesktopWindow, isKeyboardVisible = true, captured = it)
        }
        assertEquals(0, desktop.keyboardBottom)
    }

    @Test
    fun theStatusBarIconsContrastWithTheTheme() {
        val dark = render(isDarkMode = true, background = Color.Black)
        assertTrue(lightPixels(dark, 30..61, 25..37) > 0, "white clock in dark mode")
        assertEquals(0, lightPixels(dark, 0..401, 100..100), "the content below the bar is untouched")

        val light = render(isDarkMode = false, background = Color.White)
        assertTrue(darkPixels(light, 30..61, 25..37) > 0, "dark clock in light mode")
    }

    @Test
    fun theStatusBarFollowsTheLayoutDirection() {
        // The gap between the clock and the status icons in LTR; the battery in RTL.
        assertEquals(0, lightPixels(render(isDarkMode = true, background = Color.Black), 75..95, 20..45))
        val rtl = render(isDarkMode = true, background = Color.Black, layoutDirection = LayoutDirection.Rtl)
        assertTrue(lightPixels(rtl, 75..95, 20..45) > 0)
    }

    @Test
    fun theKeyboardBandIsPaintedOverTheContent() {
        assertTrue(render(isKeyboardVisible = true)[10, 800].red < 0.4f)
        assertEquals(Color.White, render()[10, 800])
    }

    @Test
    fun hidingTheSystemUiKeepsTheInsetsAndTheKeyboard() {
        val captured = Captured()
        val pixels = render(
            isSystemUiVisible = false,
            isKeyboardVisible = true,
            isDarkMode = true,
            background = Color.Black,
            captured = captured,
        )
        assertEquals(0, lightPixels(pixels, 30..61, 25..37), "no clock")
        assertEquals(335, captured.safeAreaBottom, "the safe area includes the keyboard, as with the bars shown")
        // The keyboard is its own switch: hiding the bars doesn't hide it.
        assertTrue(pixels[10, 800] != Color.Black, "the keyboard band")
        assertEquals(335, captured.keyboardBottom)
    }

    @Test
    fun composeApisDescribeTheSimulatedDevice() {
        val captured = Captured().also { render(isDarkMode = true, captured = it) }
        assertEquals(62, captured.statusBarTop)
        assertEquals(true, captured.isDark)
        // The padding modifiers read the same insets: 874 less the status bar and home indicator.
        assertEquals(874 - 62 - 34, captured.paddedHeight)

        val light = Captured().also { render(isDarkMode = false, isKeyboardVisible = true, captured = it) }
        assertEquals(false, light.isDark)
        assertEquals(874 - 62 - 335, light.paddedHeight, "the raised keyboard pads the content too")
    }

    private fun lightPixels(pixels: PixelMap, xs: IntRange, ys: IntRange): Int =
        xs.sumOf { x -> ys.count { y -> pixels[x, y].red > 0.5f } }

    private fun darkPixels(pixels: PixelMap, xs: IntRange, ys: IntRange): Int =
        xs.sumOf { x -> ys.count { y -> pixels[x, y].red < 0.5f } }
}
