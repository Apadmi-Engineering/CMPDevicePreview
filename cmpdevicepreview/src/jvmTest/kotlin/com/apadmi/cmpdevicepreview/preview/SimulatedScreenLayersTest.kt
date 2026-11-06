package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Opens dialogs, sheets and menus on a device drawn away from the centre of its window, so one laid
 * out against the window lands somewhere else than one laid out on the screen. A test that doesn't
 * zoom draws it at a scale of 1; one that does draws it as the preview does, with a graphics layer.
 */
class SimulatedScreenLayersTest {
    /** The device's screen in the window: a 400 x 800 window, 100 px in and 50 px down. */
    private val screen = Rect(100f, 50f, 500f, 850f)

    /** A device small enough to fit the scene when it is zoomed. */
    private val smallDevice = FreeformDevice(Size(300f, 400f))

    /** [smallDevice]'s screen at a zoom of 2 about its top-left corner, then moved by [pan]. */
    private val pan = Offset(150f, 40f)
    private val zoomedScreen = Rect(250f, 90f, 850f, 890f)

    private fun show(
        device: Device = FreeformDevice(Size(400f, 800f)),
        zoom: Float = 1f,
        pan: Offset = Offset.Zero,
        viewport: DpSize? = null,
        content: @Composable () -> Unit,
        check: Harness.() -> Unit
    ) {
        val harness = Harness(device, zoom, pan, viewport, content)
        try {
            harness.settle()
            harness.check()
        } finally {
            harness.scene.close()
        }
    }

    @Test
    fun aDialogIsLaidOutAsIfTheScreenWereTheWindow() {
        var bounds = Rect.Zero
        var windowSize = IntSize.Zero
        show(content = {
            Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                windowSize = LocalWindowInfo.current.containerSize
                Box(Modifier.fillMaxSize().onGloballyPositioned { bounds = it.boundsInWindow() })
            }
        }) {
            assertEquals(screen, bounds, "a dialog filling its window fills the screen")
            assertEquals(IntSize(400, 800), windowSize)
        }
    }

    @Test
    fun aDialogIsCentredOnTheScreenAtThePhonesDialogWidth() {
        var small = Rect.Zero
        var wide = Rect.Zero
        show(content = {
            Dialog(onDismissRequest = {}) {
                Box(Modifier.size(100.dp).onGloballyPositioned { small = it.boundsInWindow() })
            }
            Dialog(onDismissRequest = {}) {
                Box(Modifier.fillMaxWidth().height(10.dp).onGloballyPositioned { wide = it.boundsInWindow() })
            }
        }) {
            assertEquals(Rect(250f, 400f, 350f, 500f), small)
            // Compose's default for a screen less than 480 dp across, rather than the window's 580 dp.
            assertEquals(320f, wide.width)
            assertEquals(screen.center.x, wide.center.x)
        }
    }

    @Test
    fun onlyTheScreenIsDimmedBehindADialog() {
        show(content = {
            Dialog(onDismissRequest = {}) { Box(Modifier.size(100.dp).background(Color.White)) }
        }) {
            val pixels = settle()
            assertEquals(Color.White, pixels[50, 450], "beside the device")
            assertTrue(pixels[150, 450].red < 0.5f, "on the screen, beside the dialog")
            assertEquals(Color.White, pixels[300, 450], "the dialog")
        }
    }

    @Test
    fun aTapOnADialogReachesItAndATapBesideItDismissesIt() {
        var clicks = 0
        var dismissals = 0
        show(content = {
            Dialog(onDismissRequest = { dismissals++ }) {
                Box(Modifier.size(100.dp).clickable { clicks++ })
            }
        }) {
            tap(screen.center)
            assertEquals(1, clicks)
            assertEquals(0, dismissals)
            tap(Offset(150f, 100f))
            assertEquals(1, dismissals)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun aBottomSheetSitsAtTheBottomOfTheScreen() {
        var bounds = Rect.Zero
        show(content = {
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                Box(Modifier.fillMaxWidth().height(200.dp).onGloballyPositioned { bounds = it.boundsInWindow() })
            }
        }) {
            assertEquals(screen.left, bounds.left)
            assertEquals(screen.right, bounds.right)
            assertEquals(screen.bottom, bounds.bottom)
        }
    }

    @Test
    fun aMenuOpensWithinTheScreen() {
        var bounds = Rect.Zero
        show(content = {
            // An anchor at the screen's right edge, so a menu that doesn't fit beside it turns back.
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.TopEnd).padding(top = 100.dp).size(40.dp)) {
                    DropdownMenu(expanded = true, onDismissRequest = {}) {
                        DropdownMenuItem(
                            text = { Text("An item with a long name") },
                            onClick = {},
                            modifier = Modifier.onGloballyPositioned { bounds = it.boundsInWindow() },
                        )
                    }
                }
            }
        }) {
            assertTrue(bounds.left >= screen.left && bounds.right <= screen.right, "$bounds is on $screen")
        }
    }

    @Test
    fun aDialogIsScaledAndPositionedWithTheZoomedScreen() {
        var filling = Rect.Zero
        var centred = Rect.Zero
        var laidOut = IntSize.Zero
        show(device = smallDevice, zoom = 2f, pan = pan, content = {
            Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Box(Modifier.fillMaxSize().onGloballyPositioned {
                    filling = it.boundsInWindow()
                    laidOut = it.size
                })
            }
            Dialog(onDismissRequest = {}) {
                Box(Modifier.size(100.dp).onGloballyPositioned { centred = it.boundsInWindow() })
            }
        }) {
            assertEquals(zoomedScreen, filling)
            assertEquals(IntSize(300, 400), laidOut, "it is laid out at the screen's size and drawn zoomed")
            assertEquals(Rect(450f, 390f, 650f, 590f), centred)
        }
    }

    @Test
    fun aDialogIsNotLaidOutAgainAsThePreviewZooms() {
        var layouts = 0
        show(device = smallDevice, content = {
            Dialog(onDismissRequest = {}) {
                Box(Modifier.size(100.dp).layout { measurable, constraints ->
                    layouts++
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                })
            }
        }) {
            val before = layouts
            zoom = 2f
            pan = Offset(-30f, -20f)
            settle()
            zoom = 3f
            settle()
            assertEquals(before, layouts)
        }
    }

    @Test
    fun onlyTheZoomedScreenIsDimmedBehindADialog() {
        show(device = smallDevice, zoom = 2f, pan = pan, content = {
            Dialog(onDismissRequest = {}) { Box(Modifier.size(100.dp).background(Color.White)) }
        }) {
            val pixels = settle()
            assertEquals(Color.White, pixels[200, 450], "beside the zoomed screen, where the screen was unzoomed")
            assertEquals(Color.White, pixels[870, 450], "beside the zoomed screen, on its far side")
            assertEquals(Color.White, pixels[500, 900], "below the zoomed screen")
            assertTrue(pixels[300, 450].red < 0.5f, "on the zoomed screen, beside the dialog")
            assertTrue(pixels[800, 600].red < 0.5f, "on the zoomed screen, far from the middle")
            assertTrue(pixels[500, 850].red < 0.5f, "on the zoomed screen, near its bottom")
            assertEquals(Color.White, pixels[550, 490], "the dialog")
        }
    }

    @Test
    fun aDialogIsOnlyShownWhereThePreviewShowsTheScreen() {
        show(device = smallDevice, zoom = 2f, viewport = DpSize(400.dp, 500.dp), content = {
            Dialog(onDismissRequest = {}) { Box(Modifier.size(100.dp).background(Color.White)) }
        }) {
            val pixels = settle()
            assertTrue(pixels[300, 300].red < 0.5f, "on the screen, in the preview")
            assertEquals(Color.White, pixels[600, 300], "on the screen, past the preview's right edge")
            assertEquals(Color.White, pixels[300, 700], "on the screen, past the preview's bottom edge")
        }
    }

    @Test
    fun aDialogDoesNotDrawOverThePreviewsToolbar() {
        val harness = PreviewHarness()
        try {
            harness.zoomIn()
            val before = harness.settle()
            harness.isDialogOpen = true
            val after = harness.settle()
            assertEquals(before[44, 976], after[44, 976], "the toolbar in the corner")
            assertTrue(after[600, 300].red < 0.5f, "the screen is dimmed")
        } finally {
            harness.scene.close()
        }
    }

    @Test
    fun aTapOnTheZoomedDialogReachesItAndATapBesideItDismissesIt() {
        var clicks = 0
        var dismissals = 0
        show(device = smallDevice, zoom = 2f, pan = pan, content = {
            Dialog(onDismissRequest = { dismissals++ }) {
                Box(Modifier.size(100.dp).clickable { clicks++ })
            }
        }) {
            // Near the dialog's corner, which is outside it at a scale of 1.
            tap(Offset(640f, 580f))
            assertEquals(1, clicks)
            assertEquals(0, dismissals)
            tap(Offset(300f, 450f))
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun aDialogsBoundsFollowAZoomThatChangesWhileItIsOpen() {
        var clicks = 0
        var dismissals = 0
        show(device = smallDevice, content = {
            Dialog(onDismissRequest = { dismissals++ }) {
                Box(Modifier.size(100.dp).clickable { clicks++ })
            }
        }) {
            // About the screen's corner, so only the scale changes: the dialog grows from (200, 200)
            // to (300, 350) in the window.
            zoom = 2f
            settle()
            tap(Offset(450f, 500f))
            assertEquals(1, clicks)
            assertEquals(0, dismissals)
            tap(Offset(250f, 250f))
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun aMenuOpensWithinTheZoomedScreen() {
        var bounds = Rect.Zero
        show(device = smallDevice, zoom = 2f, pan = pan, content = {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.TopEnd).padding(top = 100.dp).size(40.dp)) {
                    DropdownMenu(expanded = true, onDismissRequest = {}) {
                        DropdownMenuItem(
                            text = { Text("An item with a long name") },
                            onClick = {},
                            modifier = Modifier.onGloballyPositioned { bounds = it.boundsInWindow() },
                        )
                    }
                }
            }
        }) {
            assertTrue(bounds.width > 200f, "$bounds is zoomed")
            assertEquals(bounds, bounds.intersect(zoomedScreen), "$bounds is on $zoomedScreen")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun aBottomSheetSitsAtTheBottomOfTheZoomedScreen() {
        var bounds = Rect.Zero
        show(device = smallDevice, zoom = 2f, pan = pan, content = {
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                Box(Modifier.fillMaxWidth().height(200.dp).onGloballyPositioned { bounds = it.boundsInWindow() })
            }
        }) {
            assertEquals(zoomedScreen.left, bounds.left)
            assertEquals(zoomedScreen.right, bounds.right)
            assertEquals(zoomedScreen.bottom, bounds.bottom)
            assertEquals(400f, bounds.height, "200 dp, zoomed")
        }
    }

    @Test
    fun aDialogsContentIsAlreadyClearOfTheScreensInsets() {
        var outside = -1
        var inside = -1
        show(device = DevicePresets.iPhone16Pro, content = {
            outside = WindowInsets.safeDrawing.getTop(LocalDensity.current)
            Dialog(onDismissRequest = {}) {
                inside = WindowInsets.safeDrawing.getTop(LocalDensity.current)
            }
        }) {
            assertTrue(outside > 0, "the screen has a status bar")
            assertEquals(0, inside, "the dialog keeps clear of it already")
        }
    }

    private abstract class SceneHarness {
        protected var clock = 0L
        abstract val scene: ImageComposeScene

        /** Renders a second of frames, long enough for a dialog or sheet to finish appearing. */
        fun settle(): PixelMap {
            repeat(60) {
                clock += 16
                scene.render(clock * 1_000_000)
            }
            return scene.render(clock * 1_000_000).toComposeImageBitmap().toPixelMap()
        }

        fun tap(at: Offset) {
            for (type in listOf(PointerEventType.Press, PointerEventType.Release)) {
                clock += 16
                scene.sendPointerEvent(type, at, timeMillis = clock, type = PointerType.Touch)
            }
            settle()
        }
    }

    /** The whole preview, with its toolbars, zoomed by the wheel as a person would. */
    private class PreviewHarness : SceneHarness() {
        var isDialogOpen by mutableStateOf(false)
        override val scene = ImageComposeScene(width = 1200, height = 1000) {
            ZoomableDevicePreview(
                SimulatedDevice(FreeformDevice(Size(1000f, 800f))),
                PreviewSettings(),
                onResize = null,
                isZoomable = true,
            ) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    if (isDialogOpen) {
                        Dialog(onDismissRequest = {}) { Box(Modifier.size(100.dp).background(Color.White)) }
                    }
                }
            }
        }

        /** Ctrl and the wheel zoom about the centre, until the screen runs under the toolbar in the corner. */
        fun zoomIn() {
            repeat(4) {
                clock += 16
                scene.sendPointerEvent(
                    PointerEventType.Scroll,
                    Offset(600f, 500f),
                    scrollDelta = Offset(0f, -50f),
                    timeMillis = clock,
                    keyboardModifiers = PointerKeyboardModifiers(isCtrlPressed = true),
                )
            }
        }
    }

    private class Harness(
        device: Device,
        initialZoom: Float,
        initialPan: Offset,
        viewport: DpSize?,
        content: @Composable () -> Unit
    ) : SceneHarness() {
        var zoom by mutableFloatStateOf(initialZoom)
        var pan by mutableStateOf(initialPan)
        override val scene = ImageComposeScene(width = 1200, height = 1000) {
            Box(Modifier.fillMaxSize().background(Color.White).padding(start = 100.dp, top = 50.dp)) {
                // The preview's own bounds, which a zoomed device runs past.
                Box(viewport?.let { Modifier.size(it).clipToBounds() } ?: Modifier) {
                    DeviceFrameView(
                        device = device,
                        modifier = Modifier
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0f, 0f)
                                scaleX = zoom
                                scaleY = zoom
                                translationX = pan.x
                                translationY = pan.y
                            }
                            .size(device.portraitSize.width.dp, device.portraitSize.height.dp),
                        configuration = DeviceConfiguration(isFrameVisible = false, isSystemUiVisible = false),
                    ) {
                        Box(Modifier.fillMaxSize().background(Color.White)) { content() }
                    }
                }
            }
        }
    }
}
