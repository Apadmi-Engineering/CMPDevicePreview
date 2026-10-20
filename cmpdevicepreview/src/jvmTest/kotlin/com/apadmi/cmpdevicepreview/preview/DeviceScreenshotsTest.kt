package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeviceScreenshotsTest {
    @Test
    fun screenshotsAreAtTheDevicesOwnResolution() {
        val screenshots = DeviceScreenshots()
        // Smaller than the device, which is scaled down to fit.
        val scene = ImageComposeScene(width = 300, height = 600) {
            CompositionLocalProvider(LocalDeviceScreenshots provides screenshots) {
                DeviceFrameView(device = DevicePresets.iPhone16Pro, modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().background(Color.Red))
                }
            }
        }
        try {
            assertFalse(screenshots.canCapture, "nothing is shown yet")
            scene.render()
            assertTrue(screenshots.canCapture)

            val screen = assertNotNull(screenshots.capture(includeFrame = false)).toPixelMap()
            assertEquals(1206, screen.width)
            assertEquals(2622, screen.height)
            assertEquals(Color.Red, screen[603, 1311])

            val device = assertNotNull(screenshots.capture(includeFrame = true)).toPixelMap()
            assertTrue(device.width > screen.width && device.height > screen.height, "the frame is around the screen")
            // Outside the frame's rounded corner.
            assertEquals(0f, device[0, 0].alpha)
        } finally {
            scene.close()
        }
        assertFalse(screenshots.canCapture, "the device is gone")
    }

    @Test
    fun aScreenshotIsEncodedAsAPngThatDecodesToTheSamePixels() {
        val image = ImageBitmap(2, 1)
        Canvas(image).drawRect(0f, 0f, 2f, 1f, Paint().apply { color = Color.Red })

        val png = assertNotNull(image.toPngBytes())
        assertEquals(listOf<Byte>(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()), png.take(4))
        val decoded = Image.makeFromEncoded(png).toComposeImageBitmap().toPixelMap()
        assertEquals(2, decoded.width)
        assertEquals(1, decoded.height)
        assertEquals(Color.Red, decoded[1, 0])
    }
}
