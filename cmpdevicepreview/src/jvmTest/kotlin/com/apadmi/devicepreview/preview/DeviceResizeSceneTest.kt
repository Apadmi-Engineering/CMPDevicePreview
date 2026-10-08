package com.apadmi.devicepreview.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.apadmi.devicepreview.core.Device
import com.apadmi.devicepreview.core.FreeformDevice
import com.apadmi.devicepreview.core.portraitScreenSize
import com.apadmi.devicepreview.devices.DevicePresets
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Drags the resize handles of [DeviceFrameView] in a scene where one pixel is one dp at scale 1. */
class DeviceResizeSceneTest {

    /** A device in a scene, the way the preview holds it: resized when it is told to be. */
    private class Harness(device: Device, val width: Int, val height: Int, isFrameVisible: Boolean = true) {
        var device by mutableStateOf(device)
        val resized = mutableListOf<FreeformDevice>()
        var contentSize = IntSize.Zero
        private var clock = 0L

        val scene = ImageComposeScene(width = width, height = height) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                DeviceFrameView(
                    device = this@Harness.device,
                    modifier = Modifier.fillMaxSize(),
                    isFrameVisible = isFrameVisible,
                    onResize = { resized += it; this@Harness.device = it },
                ) {
                    Box(Modifier.fillMaxSize().background(Color.Red).onSizeChanged { contentSize = it })
                }
            }
        }

        fun frame(): PixelMap = scene.render(clock * 1_000_000).toComposeImageBitmap().toPixelMap()

        private fun send(type: PointerEventType, at: Offset) {
            clock += 16
            scene.sendPointerEvent(type, at, timeMillis = clock, type = PointerType.Touch)
            frame()
        }

        fun press(at: Offset) = send(PointerEventType.Press, at)
        fun move(to: Offset) = send(PointerEventType.Move, to)
        fun release(at: Offset) = send(PointerEventType.Release, at)

        /** Presses at [from], moves in [steps] to [from] + [by], and returns before letting go. */
        fun dragTo(from: Offset, by: Offset, steps: Int = 4) {
            press(from)
            for (step in 1..steps) move(from + by * (step.toFloat() / steps))
        }

        fun close() = scene.close()
    }

    private fun Harness.center() = Offset(width / 2f, height / 2f)

    /** The corner of [bounds] that [handle] sits on, in a scene where the device is centred. */
    private fun Harness.at(handle: ResizeHandle, bounds: Size) =
        center() + Offset(handle.x * bounds.width / 2f, handle.y * bounds.height / 2f)

    private val bottomRight = ResizeHandle(1, 1)
    private val topLeft = ResizeHandle(-1, -1)

    // 600 x 400 dp in a 624 x 424 scene is exactly what fits once the handles have their room, so
    // it is shown at a scale of 1.
    private fun fitted() = Harness(FreeformDevice(Size(600f, 400f)), width = 624, height = 424)

    @Test
    fun aFreeformWindowIsShownAtItsSizeAndFitsTheSpace() {
        val harness = fitted()
        try {
            harness.frame()
            assertEquals(IntSize(600, 400), harness.contentSize)
        } finally {
            harness.close()
        }
    }

    @Test
    fun draggingACornerOutlinesTheNewSizeAndOnlyResizesOnRelease() {
        val harness = fitted()
        try {
            harness.frame()
            // Make room first: shrink to 400 x 200, which keeps its scale of 1 instead of fitting again.
            harness.dragTo(harness.at(bottomRight, Size(600f, 400f)), Offset(-100f, -100f))
            assertEquals(IntSize(600, 400), harness.contentSize, "mid-drag, the content is as it was")
            assertTrue(harness.resized.isEmpty(), "nothing is resized before the release")
            harness.release(harness.at(bottomRight, Size(600f, 400f)) + Offset(-100f, -100f))
            assertEquals(listOf(FreeformDevice(Size(400f, 200f))), harness.resized)
            harness.frame()
            assertEquals(IntSize(400, 200), harness.contentSize, "the device keeps the scale it was drawn at")

            // Now there is room: grow by (40, 20) px, which is (80, 40) px for a device that stays centred.
            val corner = harness.at(bottomRight, Size(400f, 200f))
            harness.dragTo(corner, Offset(40f, 20f))
            assertEquals(IntSize(400, 200), harness.contentSize, "mid-drag, the content isn't laid out again")
            val outlined = harness.frame()
            assertTrue(accentPixels(outlined) > 0, "the outline and its label are drawn")
            harness.release(corner + Offset(40f, 20f))
            assertEquals(FreeformDevice(Size(480f, 240f)), harness.resized.last())
            val grown = harness.frame()
            assertEquals(IntSize(480, 240), harness.contentSize, "grown by exactly (80, 40) px, not fitted again")
            assertEquals(0, accentPixels(grown), "the outline is gone")
        } finally {
            harness.close()
        }
    }

    @Test
    fun anEdgeResizesOneAxis() {
        val harness = fitted()
        try {
            harness.frame()
            // Shrink first, so there is room to grow either way.
            harness.dragTo(harness.at(bottomRight, Size(600f, 400f)), Offset(-100f, -100f))
            harness.release(harness.at(bottomRight, Size(600f, 400f)) + Offset(-100f, -100f))
            harness.frame()

            val right = harness.at(ResizeHandle(1, 0), Size(400f, 200f))
            // Dragging diagonally still only moves the side the edge is on.
            harness.dragTo(right, Offset(30f, 50f))
            harness.release(right + Offset(30f, 50f))
            assertEquals(FreeformDevice(Size(460f, 200f)), harness.resized.last())
            harness.frame()
            assertEquals(IntSize(460, 200), harness.contentSize)

            val top = harness.at(ResizeHandle(0, -1), Size(460f, 200f))
            harness.dragTo(top, Offset(70f, -20f))
            harness.release(top + Offset(70f, -20f))
            assertEquals(FreeformDevice(Size(460f, 240f)), harness.resized.last())
        } finally {
            harness.close()
        }
    }

    @Test
    fun aPresetBecomesAFreeformWindowFromItsScreenSize() {
        val pixel = DevicePresets.pixel9
        val harness = Harness(pixel, width = 1000, height = 700)
        try {
            harness.frame()
            val screen = pixel.portraitScreenSize
            val bounds = pixel.frame!!.bodyBounds(screen, isPortrait = true).size
            val scale = harness.contentSize.width / screen.width
            val corner = harness.at(topLeft, Size(bounds.width * scale, bounds.height * scale))

            // 30 px inwards from the top-left corner takes 60 px off each axis.
            harness.dragTo(corner, Offset(30f, 30f))
            harness.release(corner + Offset(30f, 30f))
            val resized = harness.resized.single()
            assertEquals((screen.width - 60f / scale).roundToInt().toFloat(), resized.size.width)
            assertEquals((screen.height - 60f / scale).roundToInt().toFloat(), resized.size.height)
            assertNotEquals(pixel, harness.device)
            assertTrue(harness.device is FreeformDevice)

            // The new window is shown at the scale the phone had.
            harness.frame()
            assertEquals(resized.size.width * scale, harness.contentSize.width.toFloat(), 1f)
        } finally {
            harness.close()
        }
    }

    @Test
    fun aClickOrAWiggleChangesNothing() {
        val harness = fitted()
        try {
            harness.frame()
            val corner = harness.at(bottomRight, Size(600f, 400f))
            harness.press(corner)
            harness.release(corner)
            // Within the touch slop, it isn't a drag.
            harness.press(corner)
            harness.move(corner + Offset(2f, 1f))
            harness.release(corner + Offset(2f, 1f))
            assertTrue(harness.resized.isEmpty())
            assertEquals(FreeformDevice(Size(600f, 400f)), harness.device)

            // Dragged out and back to where it started: the size is the same, so nothing is resized.
            harness.dragTo(corner, Offset(-60f, -60f))
            harness.move(corner)
            harness.release(corner)
            assertTrue(harness.resized.isEmpty())
            harness.frame()
            assertEquals(IntSize(600, 400), harness.contentSize)
        } finally {
            harness.close()
        }
    }

    @Test
    fun theSizeIsKeptWithinTheSpace() {
        val harness = fitted()
        try {
            harness.frame()
            val corner = harness.at(bottomRight, Size(600f, 400f))
            // Dragged far beyond the space the device has: it ends at what fits, not more.
            harness.dragTo(corner, Offset(-5f, -5f))
            harness.move(corner + Offset(300f, 300f))
            harness.release(corner + Offset(300f, 300f))
            assertTrue(harness.resized.isEmpty(), "600 x 400 is all that fits, so it didn't change")

            // And far in: not smaller than the smallest window.
            harness.dragTo(corner, Offset(-400f, -400f))
            harness.release(corner + Offset(-400f, -400f))
            assertEquals(FreeformDevice(Size(MinFreeformSize, MinFreeformSize)), harness.resized.single())
        } finally {
            harness.close()
        }
    }

    @Test
    fun aNewDeviceFitsTheSpaceAgain() {
        val harness = fitted()
        try {
            harness.frame()
            harness.dragTo(harness.at(bottomRight, Size(600f, 400f)), Offset(-100f, -100f))
            harness.release(harness.at(bottomRight, Size(600f, 400f)) + Offset(-100f, -100f))
            harness.frame()
            assertEquals(IntSize(400, 200), harness.contentSize, "pinned at a scale of 1")

            // A typed size is another device, shown as big as it can be: 300 x 150 at a scale of 2.
            harness.device = FreeformDevice(Size(300f, 150f))
            harness.frame()
            harness.frame()
            assertEquals(IntSize(600, 300), harness.contentSize)

            // And dragging that to the size it was before is not the pinned one any more.
            harness.device = FreeformDevice(Size(400f, 200f))
            harness.frame()
            harness.frame()
            assertEquals(IntSize(600, 300), harness.contentSize, "400 x 200 fits at a scale of 1.5")
        } finally {
            harness.close()
        }
    }

    @Test
    fun withoutAnOnResizeThereAreNoHandles() {
        var device by mutableStateOf<Device>(FreeformDevice(Size(400f, 200f)))
        var contentSize = IntSize.Zero
        val scene = ImageComposeScene(width = 424, height = 224) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                DeviceFrameView(device, Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().onSizeChanged { contentSize = it })
                }
            }
        }
        try {
            scene.render()
            // Without handles to make room, the device fills the space.
            assertEquals(IntSize(424, 212), contentSize)
            val corner = Offset(212f + 212f, 112f + 106f)
            scene.sendPointerEvent(PointerEventType.Press, corner, timeMillis = 16, type = PointerType.Touch)
            scene.sendPointerEvent(PointerEventType.Move, corner + Offset(-50f, -50f), timeMillis = 32, type = PointerType.Touch)
            scene.sendPointerEvent(PointerEventType.Release, corner + Offset(-50f, -50f), timeMillis = 48, type = PointerType.Touch)
            scene.render(48_000_000)
            assertEquals(IntSize(424, 212), contentSize)
            assertEquals(FreeformDevice(Size(400f, 200f)), device)
        } finally {
            scene.close()
        }
    }

    /** Pixels of the panel's accent colour, which only the outline and its label are drawn in. */
    private fun accentPixels(pixels: PixelMap): Int {
        val accent = PanelColors.Accent
        var count = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                if (pixels[x, y] == accent) count++
            }
        }
        return count
    }
}
