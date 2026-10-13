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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Opens dialogs, sheets and menus on a device drawn at a scale of 1, away from the centre of its
 * window, so one laid out against the window lands somewhere else than one laid out on the screen.
 */
class SimulatedScreenLayersTest {
    /** The device's screen in the window: a 400 x 800 window, 100 px in and 50 px down. */
    private val screen = Rect(100f, 50f, 500f, 850f)

    private fun show(
        device: Device = FreeformDevice(Size(400f, 800f)),
        content: @Composable () -> Unit,
        check: Harness.() -> Unit
    ) {
        val harness = Harness(device, content)
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

    private class Harness(device: Device, content: @Composable () -> Unit) {
        private var clock = 0L
        val scene = ImageComposeScene(width = 1200, height = 1000) {
            Box(Modifier.fillMaxSize().background(Color.White).padding(start = 100.dp, top = 50.dp)) {
                DeviceFrameView(
                    device = device,
                    modifier = Modifier.size(device.portraitSize.width.dp, device.portraitSize.height.dp),
                    configuration = DeviceConfiguration(isFrameVisible = false, isSystemUiVisible = false),
                ) {
                    Box(Modifier.fillMaxSize().background(Color.White)) { content() }
                }
            }
        }

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
}
