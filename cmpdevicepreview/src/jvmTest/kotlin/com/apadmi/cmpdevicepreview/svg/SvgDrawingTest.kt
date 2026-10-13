package com.apadmi.cmpdevicepreview.svg

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.apadmi.cmpdevicepreview.assertRectEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SvgDrawingTest {
    @Test
    fun readsTheViewBox() {
        assertEquals(Rect(1f, 2f, 11f, 22f), SvgDrawing.parse("""<svg viewBox="1 2 10 20"/>""").viewBox)
        assertEquals(Rect(0f, 0f, 30f, 40f), SvgDrawing.parse("""<svg width="30px" height="40"/>""").viewBox)
    }

    @Test
    fun flattensShapesInPaintOrder() {
        val drawing = SvgDrawing.parse(
            """
            <?xml version="1.0"?>
            <!-- device -->
            <svg viewBox="0 0 100 100">
              <rect x="0" y="0" width="100" height="100" rx="10" fill="#7e7e7e"/>
              <circle cx="50" cy="50" r="10" fill="#abc"/>
              <ellipse cx="50" cy="50" rx="10" ry="5" fill="rgb(255, 0, 0)"/>
              <polygon points="0,0 10,0 10,10" fill="navy"/>
              <path d="M 0,0 H 10 V 10 Z" fill="#11223344"/>
              <rect width="0" height="10"/>
            </svg>
            """.trimIndent()
        )
        assertEquals(
            listOf(Color(0xFF7E7E7E), Color(0xFFAABBCC), Color.Red, Color(0xFF000080), Color(0x44112233)),
            drawing.shapes.map { it.color },
        )
        assertEquals(Rect(40f, 40f, 60f, 60f), drawing.shapes[1].path.getBounds())
    }

    @Test
    fun appliesTransformsRightToLeftAndNested() {
        val drawing = SvgDrawing.parse(
            """
            <svg viewBox="0 0 100 100">
              <g transform="translate(5, 0)">
                <rect transform="scale(2) translate(10, 5)" width="1" height="1"/>
              </g>
              <rect transform="rotate(90 10 10)" x="10" y="10" width="2" height="1"/>
            </svg>
            """.trimIndent()
        )
        assertEquals(Rect(25f, 10f, 27f, 12f), drawing.shapes[0].path.getBounds())
        assertRectEquals(Rect(9f, 10f, 10f, 12f), drawing.shapes[1].path.getBounds(), tolerance = 0.001f)
    }

    @Test
    fun resolvesFillsAndOpacity() {
        val drawing = SvgDrawing.parse(
            """
            <svg viewBox="0 0 10 10">
              <g opacity="0.5" fill="#ff0000">
                <rect width="1" height="1"/>
                <rect width="1" height="1" fill-opacity="0.5"/>
                <rect width="1" height="1" fill="none"/>
                <rect width="1" height="1" style="fill: #00ff00; opacity: 0.5"/>
              </g>
              <rect width="1" height="1" fill="currentColor"/>
              <rect width="1" height="1"/>
            </svg>
            """.trimIndent()
        )
        val shapes = drawing.shapes
        assertEquals(5, shapes.size)
        assertEquals(Color.Red.copy(alpha = 0.5f), shapes[0].color)
        assertEquals(Color.Red.copy(alpha = 0.25f), shapes[1].color)
        assertEquals(Color.Green.copy(alpha = 0.25f), shapes[2].color)
        assertFalse(shapes[0].followsCurrentColor)
        assertTrue(shapes[3].followsCurrentColor)
        assertTrue(shapes[4].followsCurrentColor)
        assertEquals(Color.Black, shapes[4].color)
    }

    @Test
    fun currentColorIgnoresTheInheritedFill() {
        val drawing = SvgDrawing.parse(
            """<svg viewBox="0 0 10 10"><g fill="#ff000080"><rect width="1" height="1" fill="currentColor"/></g></svg>"""
        )
        val shape = drawing.shapes.single()
        assertTrue(shape.followsCurrentColor)
        // Black and opaque, so a tint isn't made translucent by the group's half-transparent red.
        assertEquals(Color.Black, shape.color)
    }

    @Test
    fun resolvesClipPaths() {
        val drawing = SvgDrawing.parse(
            """
            <svg viewBox="0 0 10 10">
              <defs>
                <clipPath id="left"><rect width="5" height="10"/></clipPath>
              </defs>
              <g clip-path="url(#left)">
                <rect width="10" height="10" clip-path="url(#left)"/>
              </g>
              <rect width="10" height="10" clip-path="url(#missing)"/>
            </svg>
            """.trimIndent()
        )
        assertEquals(2, drawing.shapes[0].clips.size)
        assertEquals(Rect(0f, 0f, 5f, 10f), drawing.shapes[0].clips[0].getBounds())
        assertEquals(0, drawing.shapes[1].clips.size)
    }

    @Test
    fun decodesXmlEntities() {
        val root = parseXmlDocument("""<svg a="&lt;&amp;&#x41;&#66;&quot;" b='x'><!-- c --><g/></svg>""")
        assertEquals("<&AB\"", root.attribute("a"))
        assertEquals("x", root.attribute("b"))
        assertEquals(listOf("g"), root.children.map { it.name })
    }

    @Test
    fun rejectsMalformedDocuments() {
        assertFailsWith<IllegalArgumentException> { SvgDrawing.parse("<svg viewBox=\"0 0 1 1\"><rect></svg>") }
        assertFailsWith<IllegalArgumentException> { SvgDrawing.parse("<g/>") }
        assertFailsWith<IllegalArgumentException> { SvgDrawing.parse("<svg/>") }
    }
}
