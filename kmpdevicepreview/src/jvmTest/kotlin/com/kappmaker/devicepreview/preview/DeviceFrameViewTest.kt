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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        var windowSize: DpSize = DpSize.Unspecified,
        var density: Float = Float.NaN,
        var fontScale: Float = Float.NaN,
        var textSizes: Map<Int, Float> = emptyMap(),
        var textHeight: Int = -1,
        var layoutDirection: LayoutDirection? = null,
    )

    private fun render(
        device: Device = iPhone,
        isSystemUiVisible: Boolean = true,
        isKeyboardVisible: Boolean = false,
        isDarkMode: Boolean = false,
        background: Color = Color.White,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        deviceLayoutDirection: LayoutDirection? = null,
        fontScale: Float = 1f,
        displayScale: Float = 1f,
        captured: Captured = Captured(),
    ): PixelMap = renderComposeScene(402, 874) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            DeviceFrameView(
                device = device,
                isFrameVisible = false,
                isSystemUiVisible = isSystemUiVisible,
                isKeyboardVisible = isKeyboardVisible,
                isDarkMode = isDarkMode,
                fontScale = fontScale,
                displayScale = displayScale,
                layoutDirection = deviceLayoutDirection ?: LocalLayoutDirection.current,
            ) {
                val density = LocalDensity.current
                captured.safeAreaBottom = WindowInsets.safeDrawing.getBottom(density)
                captured.keyboardBottom = WindowInsets.ime.getBottom(density)
                captured.statusBarTop = WindowInsets.statusBars.getTop(density)
                captured.isDark = isSystemInDarkTheme()
                captured.windowSize = LocalWindowInfo.current.containerDpSize
                captured.density = density.density
                captured.fontScale = density.fontScale
                captured.textSizes = listOf(14, 30).associateWith { with(density) { it.sp.toDp().value } }
                captured.layoutDirection = LocalLayoutDirection.current
                Box(Modifier.fillMaxSize().background(background)) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding().onSizeChanged { captured.paddedHeight = it.height })
                    BasicText("Hg", style = TextStyle(fontSize = 30.sp), modifier = Modifier.onSizeChanged { captured.textHeight = it.height })
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
        // The window is the device's screen, so window size classes follow the device.
        assertEquals(DpSize(402.dp, 874.dp), captured.windowSize)
    }

    @Test
    fun theFontScaleScalesText() {
        val scaled = Captured().also { render(fontScale = 1.3f, captured = it) }
        assertEquals(1.3f, scaled.fontScale)
        assertEquals(14f * 1.3f, scaled.textSizes.getValue(14), 0.01f)
        assertEquals(30f * 1.3f, scaled.textSizes.getValue(30), 0.01f)

        // Laying out text follows the same sizes.
        val defaultHeight = Captured().also { render(captured = it) }.textHeight.toFloat()
        val doubledHeight = Captured().also { render(fontScale = 2f, captured = it) }.textHeight.toFloat()
        assertEquals(2f, doubledHeight / defaultHeight, 0.1f)
    }

    @Test
    fun aLargerDisplaySizeShrinksTheWindowButNotTheBars() {
        val default = Captured()
        val defaultPixels = render(isDarkMode = true, background = Color.Black, captured = default)
        val larger = Captured()
        val largerPixels = render(isDarkMode = true, background = Color.Black, displayScale = 1.25f, captured = larger)

        assertEquals(DpSize(402.dp / 1.25f, 874.dp / 1.25f), larger.windowSize)
        assertEquals(1.25f, larger.density)
        // The status bar is as tall on the screen, so it is fewer of the content's dp.
        assertEquals(default.statusBarTop, larger.statusBarTop)
        assertEquals(lightPixels(defaultPixels, 0..401, 0..61), lightPixels(largerPixels, 0..401, 0..61))
    }

    @Test
    fun theDeviceHasItsOwnLayoutDirection() {
        val rtl = Captured()
        val pixels = render(isDarkMode = true, background = Color.Black, deviceLayoutDirection = LayoutDirection.Rtl, captured = rtl)
        assertEquals(LayoutDirection.Rtl, rtl.layoutDirection)
        // The status bar mirrors with it: the battery is where LTR has a gap.
        assertTrue(lightPixels(pixels, 75..95, 20..45) > 0)

        val ltr = Captured()
        val hostRtl = render(
            isDarkMode = true,
            background = Color.Black,
            layoutDirection = LayoutDirection.Rtl,
            deviceLayoutDirection = LayoutDirection.Ltr,
            captured = ltr,
        )
        assertEquals(LayoutDirection.Ltr, ltr.layoutDirection)
        assertEquals(0, lightPixels(hostRtl, 75..95, 20..45))
    }

    private fun lightPixels(pixels: PixelMap, xs: IntRange, ys: IntRange): Int =
        xs.sumOf { x -> ys.count { y -> pixels[x, y].red > 0.5f } }

    private fun darkPixels(pixels: PixelMap, xs: IntRange, ys: IntRange): Int =
        xs.sumOf { x -> ys.count { y -> pixels[x, y].red < 0.5f } }
}
