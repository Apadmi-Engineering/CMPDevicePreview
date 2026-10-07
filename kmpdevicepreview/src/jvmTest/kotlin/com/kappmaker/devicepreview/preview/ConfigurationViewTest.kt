package com.kappmaker.devicepreview.preview

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigurationViewTest {

    @Test
    fun pixelRatiosDropTrailingZeros() {
        assertEquals("3x", formatPixelRatio(3f))
        assertEquals("2.625x", formatPixelRatio(2.625f))
        assertEquals("2.75x", formatPixelRatio(2.75f))
        assertEquals("1.5x", formatPixelRatio(1.5f))
    }
}
