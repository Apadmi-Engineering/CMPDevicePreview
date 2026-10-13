package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals

class FormattingTest {
    @Test
    fun dimensionsAreInWholeDp() {
        assertEquals("402 × 874", Size(402f, 874f).dimensionsLabel())
        assertEquals("412 × 915", Size(411.6f, 915.2f).dimensionsLabel())
    }

    @Test
    fun screenshotNamesAreSafeFileNames() {
        val image = ImageBitmap(2064, 2752)
        assertEquals("iPad Pro 13 (M5) 2064x2752.png", screenshotName(DevicePresets.iPadPro13M5, image, includesFrame = false))
        assertEquals("iPad Pro 13 (M5) with frame.png", screenshotName(DevicePresets.iPadPro13M5, image, includesFrame = true))
    }

    @Test
    fun pixelRatiosDropTrailingZeros() {
        assertEquals("3x", formatPixelRatio(3f))
        assertEquals("2.625x", formatPixelRatio(2.625f))
        assertEquals("2.75x", formatPixelRatio(2.75f))
        assertEquals("1.5x", formatPixelRatio(1.5f))
    }
}
