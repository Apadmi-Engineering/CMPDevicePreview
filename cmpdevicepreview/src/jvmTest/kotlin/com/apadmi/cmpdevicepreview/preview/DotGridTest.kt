package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DotGridTest {

    @Test
    fun theGridIsCentredAndCoversTheArea() {
        val points = dotGridPoints(Size(100f, 60f), spacing = 20f)
        val xs = points.map { it.x }.distinct()
        val ys = points.map { it.y }.distinct()
        assertEquals(listOf(10f, 30f, 50f, 70f, 90f), xs)
        assertEquals(listOf(10f, 30f, 50f), ys)
        assertEquals(xs.size * ys.size, points.size)
        assertTrue(Offset(50f, 30f) in points, "a dot on the centre")
    }

    @Test
    fun nothingToDrawWithoutAnAreaOrSpacing() {
        assertEquals(emptyList(), dotGridPoints(Size.Zero, spacing = 20f))
        assertEquals(emptyList(), dotGridPoints(Size(100f, 60f), spacing = 0f))
    }
}
