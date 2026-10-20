package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class PreviewViewportTest {
    private val scenes = mutableListOf<ImageComposeScene>()

    private fun viewport() = PreviewViewport().apply { resize(Size(400f, 400f)) }

    @AfterTest
    fun closeScenes() = scenes.forEach(ImageComposeScene::close)

    @Test
    fun zoomingKeepsThePointUnderTheCentroidWhereItWas() {
        val viewport = viewport()
        viewport.zoomTo(2f, about = Offset(100f, 100f))
        // The point of the device that was at (100, 100) is still there.
        assertEquals(Offset(100f, 100f), (Offset(100f, 100f) - viewport.pan) / viewport.zoom)
        assertEquals(2f, viewport.zoom)
    }

    @Test
    fun aZoomedDeviceCannotBeDraggedOffItsEdges() {
        val viewport = viewport()
        viewport.zoomTo(2f)
        viewport.panBy(Offset(500f, 500f))
        assertEquals(Offset.Zero, viewport.pan)
        viewport.panBy(Offset(-900f, -900f))
        assertEquals(Offset(-400f, -400f), viewport.pan)
    }

    @Test
    fun aDeviceSmallerThanTheSpaceStaysCentredAndCannotBeDragged() {
        val viewport = viewport()
        viewport.zoomTo(0.5f)
        assertEquals(Offset(100f, 100f), viewport.pan)
        assertFalse(viewport.canPan)
        viewport.panBy(Offset(50f, 50f))
        assertEquals(Offset(100f, 100f), viewport.pan)
    }

    @Test
    fun zoomIsKeptBetweenItsLimits() {
        val viewport = viewport()
        viewport.zoomTo(1000f)
        assertEquals(8f, viewport.zoom)
        viewport.zoomTo(0.0001f)
        assertEquals(0.25f, viewport.zoom)
    }

    @Test
    fun zoomingToAZoomLandsExactlyOnIt() {
        val viewport = viewport()
        viewport.zoomTo(1.3f)
        viewport.zoomTo(1.5f)
        assertEquals(1.5f, viewport.zoom)
    }

    @Test
    fun resettingFitsTheDeviceAgain() {
        val viewport = viewport()
        viewport.zoomTo(3f, about = Offset(300f, 50f))
        viewport.reset()
        assertEquals(1f, viewport.zoom)
        assertEquals(Offset.Zero, viewport.pan)
        assertFalse(viewport.isTransformed)
    }

    @Test
    fun twoFingersApartZoomEvenOverTheDevicesOwnScrollingContent() {
        val gestures = Gestures(viewport())
        gestures.send(PointerEventType.Press, listOf(Offset(180f, 200f), Offset(220f, 200f)))
        for (step in 1..4) {
            gestures.send(PointerEventType.Move, listOf(Offset(180f - 10f * step, 200f), Offset(220f + 10f * step, 200f)))
        }
        assertTrue(gestures.viewport.zoom > 1.5f, "zoomed: ${gestures.viewport.zoom}")
    }

    @Test
    fun theWheelWithControlZoomsAndWithoutLeavesTheViewToTheDevice() {
        val gestures = Gestures(viewport())
        val control = PointerKeyboardModifiers(isCtrlPressed = true)
        gestures.send(PointerEventType.Scroll, listOf(Offset(200f, 200f)), modifiers = control, scroll = Offset(0f, -1f))
        assertTrue(gestures.viewport.zoom > 1f, "zoomed in")
        val zoomed = gestures.viewport.pan

        gestures.send(PointerEventType.Scroll, listOf(Offset(200f, 200f)), scroll = Offset(0f, 1f))
        assertEquals(zoomed, gestures.viewport.pan)
    }

    @Test
    fun aDragTheDeviceUsesDoesNotMoveTheView() {
        val gestures = Gestures(viewport())
        gestures.viewport.zoomTo(2f)
        val before = gestures.viewport.pan
        gestures.send(PointerEventType.Press, listOf(Offset(200f, 200f)))
        for (step in 1..4) {
            gestures.send(PointerEventType.Move, listOf(Offset(200f, 200f - 20f * step)))
        }
        gestures.send(PointerEventType.Release, listOf(Offset(200f, 120f)))
        assertEquals(before, gestures.viewport.pan)
    }

    @Test
    fun aDragNothingElseUsesPansAZoomedViewButNotAFittedOne() {
        val gestures = Gestures(viewport(), hasScrollingContent = false)
        val drag = {
            gestures.send(PointerEventType.Press, listOf(Offset(200f, 200f)))
            for (step in 1..4) {
                gestures.send(PointerEventType.Move, listOf(Offset(200f - 20f * step, 200f - 20f * step)))
            }
            gestures.send(PointerEventType.Release, listOf(Offset(120f, 120f)))
        }
        drag()
        assertEquals(Offset.Zero, gestures.viewport.pan, "a fitted device stays put")

        gestures.viewport.zoomTo(2f)
        val before = gestures.viewport.pan
        drag()
        assertTrue(gestures.viewport.pan.x < before.x && gestures.viewport.pan.y < before.y, "panned: ${gestures.viewport.pan}")
    }

    @Test
    fun theWheelPansAZoomedViewWhereNothingElseUsesIt() {
        val gestures = Gestures(viewport(), hasScrollingContent = false)
        gestures.viewport.zoomTo(2f)
        gestures.send(PointerEventType.Scroll, listOf(Offset(200f, 200f)), scroll = Offset(0f, 1f))
        assertTrue(gestures.viewport.pan.y < 0f, "panned down: ${gestures.viewport.pan}")
    }

    // With something of its own to scroll, which gets a drag or the wheel before the viewport does.
    private inner class Gestures(val viewport: PreviewViewport, hasScrollingContent: Boolean = true) {
        private var clock = 0L
        private val scene = ImageComposeScene(400, 400) {
            Box(Modifier.fillMaxSize().zoomAndPan(viewport)) {
                if (hasScrollingContent) {
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Box(Modifier.fillMaxSize().height(2000.dp))
                    }
                }
            }
        }

        init {
            scenes += scene
            viewport.resize(Size(400f, 400f))
            scene.render(0)
        }

        fun send(
            type: PointerEventType,
            fingers: List<Offset>,
            modifiers: PointerKeyboardModifiers = PointerKeyboardModifiers(),
            scroll: Offset = Offset.Zero,
        ) {
            clock += 16
            val pointers = fingers.mapIndexed { index, at ->
                ComposeScenePointer(PointerId(index.toLong()), at, type != PointerEventType.Release, PointerType.Touch)
            }
            scene.sendPointerEvent(type, pointers, keyboardModifiers = modifiers, scrollDelta = scroll, timeMillis = clock)
            scene.render(clock * 1_000_000)
        }
    }
}
