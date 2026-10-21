package com.apadmi.cmpdevicepreview.preview

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
import androidx.compose.ui.geometry.Size
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
import com.apadmi.cmpdevicepreview.core.Appearance
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.DeviceType
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Renders [DeviceFrameView] without its body, in a scene exactly the size of the iPhone 16 Pro's
 * screen, so one scene pixel is one logical pixel of the device.
 */
class DeviceFrameViewTest {
    private val iPhone = DevicePresets.iPhone16Pro

    @Suppress("TOO_MANY_PARAMETERS")  // Each test names only the few settings it changes.
    private fun render(
        device: Device = iPhone,
        isSystemUiVisible: Boolean = true,
        isKeyboardVisible: Boolean = false,
        appearance: Appearance = Appearance.Light,
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
                configuration = DeviceConfiguration(
                    isFrameVisible = false,
                    isSystemUiVisible = isSystemUiVisible,
                    isKeyboardVisible = isKeyboardVisible,
                    appearance = appearance,
                    fontScale = fontScale,
                    displayScale = displayScale,
                    layoutDirection = deviceLayoutDirection,
                ),
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
    }

    @Test
    fun withoutAKeyboardTheSafeAreaIsTheDevicesOwn() {
        val hidden = Captured().also { render(captured = it) }
        assertEquals(34, hidden.safeAreaBottom)
        assertEquals(0, hidden.keyboardBottom)
    }

    @Test
    fun aDesktopWindowHasNoKeyboardToRaise() {
        val desktop = Captured().also {
            render(device = DevicePresets.largeDesktopWindow, isKeyboardVisible = true, captured = it)
        }
        assertEquals(0, desktop.keyboardBottom)
    }

    @Test
    fun theStatusBarIconsContrastWithTheTheme() {
        val dark = render(appearance = Appearance.Dark, background = Color.Black)
        assertTrue(lightPixels(dark, 30..61, 25..37) > 0, "white clock in dark mode")
        assertEquals(0, lightPixels(dark, 0..401, 100..100), "the content below the bar is untouched")

        val light = render(appearance = Appearance.Light, background = Color.White)
        assertTrue(darkPixels(light, 30..61, 25..37) > 0, "dark clock in light mode")
    }

    @Test
    fun theStatusBarFollowsTheLayoutDirection() {
        // The gap between the clock and the status icons in LTR; the battery in RTL.
        assertEquals(0, lightPixels(render(appearance = Appearance.Dark, background = Color.Black), 75..95, 20..45))
        val rtl = render(appearance = Appearance.Dark, background = Color.Black, layoutDirection = LayoutDirection.Rtl)
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
            appearance = Appearance.Dark,
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
        val captured = Captured().also { render(appearance = Appearance.Dark, captured = it) }
        assertEquals(62, captured.statusBarTop)
        assertEquals(true, captured.isDark)
        // The padding modifiers read the same insets: 874 less the status bar and home indicator.
        assertEquals(874 - 62 - 34, captured.paddedHeight)

        val light = Captured().also { render(appearance = Appearance.Light, isKeyboardVisible = true, captured = it) }
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
        val defaultPixels = render(appearance = Appearance.Dark, background = Color.Black, captured = default)
        val larger = Captured()
        val largerPixels = render(appearance = Appearance.Dark, background = Color.Black, displayScale = 1.25f, captured = larger)

        assertEquals(DpSize(402.dp / 1.25f, 874.dp / 1.25f), larger.windowSize)
        assertEquals(1.25f, larger.density)
        // The status bar is as tall on the screen, so it is fewer of the content's dp.
        assertEquals(default.statusBarTop, larger.statusBarTop)
        assertEquals(lightPixels(defaultPixels, 0..401, 0..61), lightPixels(largerPixels, 0..401, 0..61))
    }

    @Test
    fun theDeviceHasItsOwnLayoutDirection() {
        val rtl = Captured()
        val pixels = render(appearance = Appearance.Dark, background = Color.Black, deviceLayoutDirection = LayoutDirection.Rtl, captured = rtl)
        assertEquals(LayoutDirection.Rtl, rtl.layoutDirection)
        // The status bar mirrors with it: the battery is where LTR has a gap.
        assertTrue(lightPixels(pixels, 75..95, 20..45) > 0)

        val ltr = Captured()
        val hostRtl = render(
            appearance = Appearance.Dark,
            background = Color.Black,
            layoutDirection = LayoutDirection.Rtl,
            deviceLayoutDirection = LayoutDirection.Ltr,
            captured = ltr,
        )
        assertEquals(LayoutDirection.Ltr, ltr.layoutDirection)
        assertEquals(0, lightPixels(hostRtl, 75..95, 20..45))
    }

    @Test
    fun aDeviceWithoutArtworkIsDrawnAsABorderedWindow() {
        val window = object : Device {
            override val name = "Window"
            override val type = DeviceType.Other
            override val portraitSize = Size(200f, 100f)

            @Suppress("NULLABLE_PROPERTY_TYPE")  // Null means the host's density; the autofix would make it 0.
            override val devicePixelRatio: Float? = null
        }
        // Fitted at a scale of 1.2: 240 x 120, from y = 10.
        val pixels = renderComposeScene(240, 140) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                DeviceFrameView(window, Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().background(Color.White))
                }
            }
        }.toComposeImageBitmap().toPixelMap()
        assertTrue(pixels[120, 10].red in 0.5f..0.7f, "a grey border, not a black one: ${pixels[120, 10]}")
        assertEquals(Color.White, pixels[120, 70], "the content")
    }

    private fun lightPixels(
        pixels: PixelMap,
        xs: IntRange,
        ys: IntRange
    ): Int =
        xs.sumOf { x -> ys.count { y -> pixels[x, y].red > 0.5f } }

    private fun darkPixels(
        pixels: PixelMap,
        xs: IntRange,
        ys: IntRange
    ): Int =
        xs.sumOf { x -> ys.count { y -> pixels[x, y].red < 0.5f } }

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
}
