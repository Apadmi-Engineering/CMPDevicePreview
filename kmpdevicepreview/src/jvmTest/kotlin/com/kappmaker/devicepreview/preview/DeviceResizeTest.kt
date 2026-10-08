package com.kappmaker.devicepreview.preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeviceResizeTest {

    private val start = Size(400f, 600f)
    private val room = Size(2000f, 2000f)
    private val bottomRight = ResizeHandle(1, 1)
    private val topLeft = ResizeHandle(-1, -1)

    @Test
    fun aCornerResizesTheDeviceOnBothSidesAtOnce() {
        // The device stays centred, so dragging 40 px right grows it 40 px each way: 80 in all.
        assertEquals(Size(480f, 640f), resizedScreenSize(start, bottomRight, Offset(40f, 20f), 1f, room))
        // The opposite corner moves the same way: outwards is up and to the left.
        assertEquals(Size(480f, 640f), resizedScreenSize(start, topLeft, Offset(-40f, -20f), 1f, room))
        // The other diagonal.
        assertEquals(Size(480f, 560f), resizedScreenSize(start, ResizeHandle(1, -1), Offset(40f, 20f), 1f, room))
        // In dp: at twice the scale, a pixel is half a dp.
        assertEquals(Size(440f, 620f), resizedScreenSize(start, bottomRight, Offset(40f, 20f), 2f, room))
        assertEquals(start, resizedScreenSize(start, bottomRight, Offset.Zero, 1f, room))
    }

    @Test
    fun anEdgeOnlyChangesItsOwnAxis() {
        val drag = Offset(40f, 25f)
        assertEquals(Size(480f, 600f), resizedScreenSize(start, ResizeHandle(1, 0), drag, 1f, room))
        assertEquals(Size(320f, 600f), resizedScreenSize(start, ResizeHandle(-1, 0), drag, 1f, room))
        assertEquals(Size(400f, 650f), resizedScreenSize(start, ResizeHandle(0, 1), drag, 1f, room))
        assertEquals(Size(400f, 550f), resizedScreenSize(start, ResizeHandle(0, -1), drag, 1f, room))
        // The side that isn't dragged isn't even rounded.
        val fractional = Size(411.4f, 915.2f)
        assertEquals(Size(411.4f, 965f), resizedScreenSize(fractional, ResizeHandle(0, 1), drag, 1f, room))
    }

    @Test
    fun theSizeIsWholeDp() {
        // 2 * 10 / 3 = 6.67
        assertEquals(Size(407f, 600f), resizedScreenSize(start, ResizeHandle(1, 0), Offset(10f, 0f), 3f, room))
        // 2 * 4 / 3 = 2.67 down
        assertEquals(Size(397f, 600f), resizedScreenSize(start, ResizeHandle(1, 0), Offset(-4f, 0f), 3f, room))
        // A corner rounds both sides, from a fractional start: 411.6 and 915.4.
        assertEquals(Size(412f, 915f), resizedScreenSize(Size(411.4f, 915.2f), bottomRight, Offset(0.1f, 0.1f), 1f, room))
    }

    @Test
    fun theSizeIsKeptBetweenTheSmallestAndWhatFits() {
        assertEquals(Size(MinFreeformSize, MinFreeformSize), resizedScreenSize(start, bottomRight, Offset(-900f, -900f), 1f, room))
        assertEquals(Size(1000f, 1500f), resizedScreenSize(start, bottomRight, Offset(5000f, 5000f), 1f, Size(1000f, 1500f)))
        // What fits is whole dp too.
        assertEquals(Size(1000f, 1500f), resizedScreenSize(start, bottomRight, Offset(5000f, 5000f), 1f, Size(1000.7f, 1500.2f)))
        // And never more than the largest window, even with room to spare or none to measure.
        assertEquals(
            Size(MaxFreeformSize, MaxFreeformSize),
            resizedScreenSize(start, bottomRight, Offset(1e6f, 1e6f), 1f, Size(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)),
        )
        // A window that is already larger than what fits doesn't jump when it is dragged inwards.
        assertEquals(Size(600f, 600f), resizedScreenSize(Size(600f, 600f), bottomRight, Offset.Zero, 1f, Size(500f, 500f)))
        // Room smaller than the smallest window still allows the smallest.
        assertEquals(Size(MinFreeformSize, MinFreeformSize), resizedScreenSize(start, bottomRight, Offset(-900f, -900f), 1f, Size(50f, 50f)))
    }

    @Test
    fun aHandleIsACornerOrAnEdge() {
        assertFailsWith<IllegalArgumentException> { ResizeHandle(0, 0) }
        assertFailsWith<IllegalArgumentException> { ResizeHandle(2, 0) }
        assertEquals(listOf(true, true, true, true), ResizeHandle.Corners.map { it.isCorner })
        assertEquals(listOf(false, false, false, false), ResizeHandle.Edges.map { it.isCorner })
    }
}
