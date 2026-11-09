package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.DeviceOrientation
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
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

    @Test
    fun aScreenshotHasAnOpenDialogOverTheDimmedScreen() {
        show(content = { BlueDialog() }) {
            val screen = screen()
            assertEquals(Color.Blue, screen[200, 400], "the dialog")
            assertTrue(screen[10, 10].red in 0.1f..0.9f, "the screen beside it, dimmed: ${screen[10, 10]}")
        }
    }

    @Test
    fun aScreenshotWithTheFrameDimsOnlyTheScreen() {
        var isDialogOpen by mutableStateOf(false)
        show(device = DevicePresets.iPhone16Pro, configuration = DeviceConfiguration(isSystemUiVisible = false), content = {
            if (isDialogOpen) {
                BlueDialog()
            }
        }) {
            val before = device()
            isDialogOpen = true
            settle()
            val after = device()
            // A pixel more, for the screen's anti-aliased edge.
            val screen = before.boundsOf(Color.Red).inflate(1)
            assertTrue(after.boundsOf(Color.Blue).width > 0, "the dialog")
            assertEquals(screen, screen.union(before.boundsOfChangesTo(after)), "only the screen changed")
        }
    }

    @Test
    fun aDialogLooksTheSameInScreenshotsAtAnyZoomOrPan() {
        var topLeft = Offset.Zero
        show(isZoomable = true, content = {
            Box(Modifier.fillMaxSize().onGloballyPositioned { topLeft = it.localToWindow(Offset.Zero) })
            BlueDialog()
        }) {
            val fitted = screen()
            // On the screen beside the dialog, where the wheel moves the preview rather than the app under it.
            val beside = Offset(450f, 300f)
            repeat(4) { scroll(beside, Offset(0f, -50f), isCtrlPressed = true) }
            settle()
            val zoomed = topLeft
            scroll(beside, Offset(100f, 100f))
            settle()
            assertTrue(zoomed.y < 0f, "part of the screen is out of view: $zoomed")
            assertTrue(topLeft != zoomed, "the preview moved: $topLeft")
            assertEquals(Color.Blue, fitted[200, 400])
            assertEquals(0, fitted.boundsOfChangesTo(screen()).width, "the screenshot is unchanged")
        }
    }

    @Test
    fun aScreenshotShowsTheDialogAsItIsNow() {
        show(content = {
            Dialog(onDismissRequest = {}) {
                var isTapped by remember { mutableStateOf(false) }
                Box(
                    Modifier
                        .size(100.dp)
                        .background(if (isTapped) Color.Green else Color.Blue)
                        .clickable(interactionSource = null, indication = null) { isTapped = true }
                )
            }
        }) {
            assertEquals(Color.Blue, screen()[200, 400])
            tap(Offset(600f, 500f))
            assertEquals(Color.Green, screen()[200, 400], "as it was tapped")
        }
    }

    @Test
    fun aScreenshotHasAMenuOpenedFromADialog() {
        show(content = {
            Dialog(onDismissRequest = {}) {
                Box(Modifier.size(200.dp).background(Color.Blue)) {
                    Box(Modifier.size(20.dp)) {
                        DropdownMenu(expanded = true, onDismissRequest = {}, containerColor = Color.Green) {
                            Box(Modifier.size(50.dp))
                        }
                    }
                }
            }
        }) {
            val screen = screen()
            val dialog = screen.boundsOf(Color.Blue)
            val menu = screen.boundsOf(Color.Green)
            assertTrue(menu.width > 0, "the menu")
            assertEquals(dialog, dialog.union(menu), "over the dialog")
        }
    }

    @Test
    fun aLandscapeScreenshotHasTheDialogInTheMiddle() {
        val landscape = DeviceConfiguration(
            orientation = DeviceOrientation.Landscape,
            isFrameVisible = false,
            isSystemUiVisible = false,
        )
        show(device = DevicePresets.iPhone16Pro, configuration = landscape, content = { BlueDialog() }) {
            val screen = screen()
            assertEquals(2622, screen.width)
            assertEquals(Color.Blue, screen[1311, 603])
        }
    }

    @Test
    fun aClosedDialogIsLeftOutOfScreenshots() {
        var isDialogOpen by mutableStateOf(true)
        show(content = {
            if (isDialogOpen) {
                BlueDialog()
            }
        }) {
            assertEquals(Color.Blue, screen()[200, 400])
            isDialogOpen = false
            settle()
            assertEquals(0, screen().boundsOf(Color.Blue).width)
            assertEquals(Color.Red, screen()[10, 10], "and its scrim")
        }
    }

    private fun show(
        device: Device = FreeformDevice(Size(400f, 800f)),
        configuration: DeviceConfiguration = DeviceConfiguration(isFrameVisible = false, isSystemUiVisible = false),
        isZoomable: Boolean = false,
        content: @Composable () -> Unit,
        check: Harness.() -> Unit
    ) {
        val harness = Harness(SimulatedDevice(device, configuration), isZoomable, content)
        try {
            harness.settle()
            harness.check()
        } finally {
            harness.scene.close()
        }
    }

    /** The smallest rectangle around the pixels of [color], or an empty one when there are none. */
    private fun PixelMap.boundsOf(color: Color): IntRect = boundsWhere { x, y -> this[x, y] == color }

    private fun PixelMap.boundsOfChangesTo(other: PixelMap): IntRect = boundsWhere { x, y -> this[x, y] != other[x, y] }

    private fun PixelMap.boundsWhere(isIn: (x: Int, y: Int) -> Boolean): IntRect {
        var bounds: IntRect? = null
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (isIn(x, y)) {
                    val pixel = IntRect(x, y, x + 1, y + 1)
                    bounds = bounds?.union(pixel) ?: pixel
                }
            }
        }
        return bounds ?: IntRect.Zero
    }

    private fun IntRect.union(other: IntRect): IntRect =
        IntRect(minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom))

    /** A device on a red screen, recording its screenshots, alone or in the preview that zooms it. */
    private class Harness(
        simulated: SimulatedDevice,
        isZoomable: Boolean,
        content: @Composable () -> Unit
    ) : SceneHarness() {
        private val screenshots = DeviceScreenshots()
        override val scene = ImageComposeScene(width = 1200, height = 1000) {
            CompositionLocalProvider(LocalDeviceScreenshots provides screenshots) {
                val onScreen = @Composable { Box(Modifier.fillMaxSize().background(Color.Red)) { content() } }
                if (isZoomable) {
                    ZoomableDevicePreview(simulated, PreviewSettings(), onResize = null, isZoomable = true, content = onScreen)
                } else {
                    DeviceFrameView(simulated.device, Modifier.fillMaxSize(), simulated.configuration, content = onScreen)
                }
            }
        }

        fun screen(): PixelMap = assertNotNull(screenshots.capture(includeFrame = false)).toPixelMap()

        fun device(): PixelMap = assertNotNull(screenshots.capture(includeFrame = true)).toPixelMap()
    }
}

@Composable
private fun BlueDialog() {
    Dialog(onDismissRequest = {}) { Box(Modifier.size(100.dp).background(Color.Blue)) }
}
