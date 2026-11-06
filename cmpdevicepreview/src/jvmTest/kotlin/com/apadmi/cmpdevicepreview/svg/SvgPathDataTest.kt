package com.apadmi.cmpdevicepreview.svg

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathHitTester
import androidx.compose.ui.graphics.addSvg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SvgPathDataTest {
    @Test
    fun anOpenPathIsWrittenAsPathData() {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(10.5f, -2f)
        }
        assertEquals("M0.0 0.0L10.5 -2.0", path.toSvgPathData())
    }

    @Test
    fun roundedCornersAreKeptAsCurves() {
        val rounded = Path().apply { addRoundRect(RoundRect(Rect(0f, 0f, 100f, 100f), CornerRadius(20f))) }
        val written = PathHitTester(Path().apply { addSvg(rounded.toSvgPathData()) })
        // Along the diagonal through the top-left corner's centre, at 18 and 22 from it.
        assertTrue(Offset(7.27f, 7.27f) in written)
        assertFalse(Offset(4.44f, 4.44f) in written)
        assertTrue(Offset(50f, 50f) in written)
    }

    @Test
    fun anEvenOddPathKeepsItsFillRule() {
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, 10f, 10f))
        }
        assertTrue(path.toCssClipPath().startsWith("path(evenodd, \"M"))
        assertTrue(Path().apply { addRect(Rect(0f, 0f, 10f, 10f)) }.toCssClipPath().startsWith("path(nonzero, \"M"))
    }
}
