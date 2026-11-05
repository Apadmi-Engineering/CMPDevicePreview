package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathHitTester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.addSvg
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.apadmi.cmpdevicepreview.svg.Affine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HtmlElementsTest {
    private val screenSize = Size(100f, 200f)
    private val nodeSize = Size(200f, 400f)
    private val screenOutline = Outline.Rectangle(Rect(Offset.Zero, nodeSize))

    // The node's pixels, at twice the screen's units.
    private fun uncovered(vararg covers: Rect) = PathHitTester(uncoveredScreen(screenOutline, covers.toList(), screenSize, nodeSize))

    @Test
    fun elementsAreScaledFromTheDevicesDensityToTheHosts() {
        val style = htmlElementsStyle(Path(), Affine.Identity, simulatedDensity = 1.5f, hostDensity = 2f)
        assertEquals("scale(0.75)", style.transform)
    }

    @Test
    fun theClipIsInTheUnitsComposePlacesElementsIn() {
        val visible = Path().apply { addRect(Rect(0f, 0f, 300f, 600f)) }
        val style = htmlElementsStyle(visible, Affine.translate(100f, 50f), simulatedDensity = 2f, hostDensity = 2f)
        assertEquals(Rect(50f, 25f, 200f, 325f), clipOf(style).getBounds())
    }

    @Test
    fun whatTheSystemUiDrawsOverIsCutFromTheScreen() {
        val visible = uncovered(Rect(0f, 0f, 100f, 20f))
        assertFalse(Offset(100f, 20f) in visible)
        assertTrue(Offset(100f, 60f) in visible)
    }

    @Test
    fun aPillOverTheKeyboardIsCutOnce() {
        val visible = uncovered(Rect(0f, 150f, 100f, 200f), Rect(0f, 180f, 100f, 200f))
        assertFalse(Offset(100f, 380f) in visible)
        assertFalse(Offset(100f, 340f) in visible)
        assertTrue(Offset(100f, 200f) in visible)
    }

    @Test
    fun elementsAreHiddenWhileTheDeviceTurns() {
        assertEquals(HiddenClipPath, htmlElementsStyle(null, Affine.Identity, simulatedDensity = 2f, hostDensity = 2f).clipPath)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theClipFollowsTheScreenThroughAZoom() = runComposeUiTest {
        val container = RecordingContainer()
        val screen = HtmlElementsScreen(Size(50f, 25f), RectangleShape, emptyList(), simulatedDensity = 2f, isTurning = false)
        setContent {
            Box(
                Modifier.graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = 2f
                    scaleY = 2f
                    translationX = 10f
                    translationY = 20f
                }
            ) {
                Box(Modifier.offset { IntOffset(30, 40) }.size(100.dp, 50.dp).htmlElementsOnScreen(container, screen))
            }
        }
        waitForIdle()
        // At (70, 100) to (270, 200) in the window, over the simulated density.
        assertEquals(Rect(35f, 50f, 135f, 100f), clipOf(assertNotNull(container.style)).getBounds())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theContainerIsRestoredWhenTheScreenLeaves() = runComposeUiTest {
        val container = RecordingContainer()
        val screen = HtmlElementsScreen(Size(10f, 10f), RectangleShape, emptyList(), simulatedDensity = 1f, isTurning = false)
        var isShown by mutableStateOf(true)
        setContent {
            if (isShown) {
                Box(Modifier.size(10.dp).htmlElementsOnScreen(container, screen))
            }
        }
        waitForIdle()
        assertNotNull(container.style)
        isShown = false
        waitForIdle()
        assertNull(container.style)
    }
}

private class RecordingContainer : HtmlElementsContainer {
    var style: HtmlElementsStyle? = null

    @Composable
    override fun Marker() = Unit

    override fun restyle(style: HtmlElementsStyle?) {
        this.style = style
    }
}

private fun clipOf(style: HtmlElementsStyle): Path {
    val (fillRule, pathData) = assertNotNull(Regex("""path\((\w+), "(.*)"\)""").matchEntire(style.clipPath)).destructured
    return Path().apply {
        addSvg(pathData)
        fillType = if (fillRule == "evenodd") PathFillType.EvenOdd else PathFillType.NonZero
    }
}
