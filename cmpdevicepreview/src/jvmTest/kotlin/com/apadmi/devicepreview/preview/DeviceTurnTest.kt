package com.apadmi.devicepreview.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import com.apadmi.devicepreview.core.Device
import com.apadmi.devicepreview.devices.DevicePresets
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceTurnTest {

    private val portrait = Size(100f, 200f)

    @Test
    fun aTurnedDeviceIsNeverLargerThanInPortrait() {
        // Wide space: landscape would fit at 4, but the device keeps its portrait scale.
        val wide = Constraints.fixed(800, 400)
        assertEquals(2f, turnedFitScale(wide, portrait, degrees = 0f, fallback = 1f))
        assertEquals(2f, turnedFitScale(wide, portrait, degrees = -90f, fallback = 1f))
        // Turned 45 degrees, its diagonal needs a little more room.
        assertTrue(turnedFitScale(wide, portrait, degrees = -45f, fallback = 1f) < 2f)

        // Narrow space: landscape has to shrink to fit.
        val narrow = Constraints.fixed(200, 400)
        assertEquals(2f, turnedFitScale(narrow, portrait, degrees = 0f, fallback = 1f))
        assertEquals(1f, turnedFitScale(narrow, portrait, degrees = -90f, fallback = 1f))

        assertEquals(3f, turnedFitScale(Constraints(), portrait, degrees = -90f, fallback = 3f))
    }

    @Test
    fun theDeviceTurnsBeforeTheContentChangesOrientation() {
        var isPortrait by mutableStateOf(true)
        var contentSize = IntSize.Zero
        val scene = ImageComposeScene(width = 1000, height = 600) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                DeviceFrameView(DevicePresets.pixel9, Modifier.fillMaxSize(), isPortrait = isPortrait) {
                    Box(Modifier.fillMaxSize().background(Color.Red).onSizeChanged { contentSize = it })
                }
            }
        }
        try {
            val upright = scene.frame(0)
            val uprightBox = deviceBox(upright)
            assertTrue(contentSize.height > contentSize.width, "portrait content")

            isPortrait = false
            scene.frame(0)
            scene.frame(16)
            val halfway = scene.frame(16 + 225)
            assertTrue(contentSize.height > contentSize.width, "mid-turn, the content is still portrait")
            assertTrue(deviceBox(halfway).width > uprightBox.width * 2, "mid-turn, the device is diagonal")

            scene.frame(16 + 500)
            val turned = scene.frame(16 + 516)
            assertTrue(contentSize.width > contentSize.height, "after the turn, the content is landscape")
            // The space is wide enough for either orientation, so the device keeps its size.
            val turnedBox = deviceBox(turned)
            assertTrue(abs(turnedBox.width - uprightBox.height) <= 2, "$turnedBox vs $uprightBox")
            assertTrue(abs(turnedBox.height - uprightBox.width) <= 2, "$turnedBox vs $uprightBox")
        } finally {
            scene.close()
        }
    }

    @Test
    fun aDesktopWindowStaysLandscapeWhateverIsAsked() {
        var isPortrait by mutableStateOf(true)
        var device by mutableStateOf<Device>(DevicePresets.largeDesktopWindow)
        var contentSize = IntSize.Zero
        val scene = ImageComposeScene(width = 1000, height = 600) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                DeviceFrameView(device, Modifier.fillMaxSize(), isPortrait = isPortrait) {
                    Box(Modifier.fillMaxSize().background(Color.Red).onSizeChanged { contentSize = it })
                }
            }
        }
        try {
            scene.frame(0)
            assertTrue(contentSize.width > contentSize.height, "a desktop window is wide")
            val upright = deviceBox(scene.frame(16))

            // Asking for portrait neither turns the window nor lays it out tall, not even mid-turn.
            isPortrait = false
            for (millis in listOf(32L, 32L + 225, 32L + 500, 32L + 516)) {
                val pixels = scene.frame(millis)
                assertTrue(contentSize.width > contentSize.height, "still wide at ${millis}ms")
                assertEquals(upright, deviceBox(pixels), "the window doesn't move at ${millis}ms")
            }

            // Swapping to a phone that is asked to be in landscape shows it in landscape, not turning.
            device = DevicePresets.pixel9
            scene.frame(600)
            assertTrue(contentSize.width > contentSize.height, "the phone is in landscape")
            // And back: a desktop window straight away, without turning from the phone's orientation.
            device = DevicePresets.largeDesktopWindow
            scene.frame(616)
            assertTrue(contentSize.width > contentSize.height, "the window is wide at once")
            assertEquals(upright, deviceBox(scene.frame(632)))
        } finally {
            scene.close()
        }
    }

    private fun ImageComposeScene.frame(millis: Long): PixelMap =
        render(millis * 1_000_000).toComposeImageBitmap().toPixelMap()

    /** The size of the box around everything that isn't the white background. */
    private fun deviceBox(pixels: PixelMap): IntSize {
        var left = pixels.width
        var top = pixels.height
        var right = -1
        var bottom = -1
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                if (pixels[x, y] != Color.White) {
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x)
                    bottom = maxOf(bottom, y)
                }
            }
        }
        return IntSize(right - left + 1, bottom - top + 1)
    }
}
