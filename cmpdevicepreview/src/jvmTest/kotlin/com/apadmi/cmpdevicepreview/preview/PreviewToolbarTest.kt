package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp

import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.devices.DevicePresets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PreviewToolbarTest {
    // A device that is half its actual size when fitted, so actual size is a zoom of 2.
    private fun viewport() = PreviewViewport().apply {
        resize(Size(400f, 400f))
        fitScale = 0.5f
    }

    @Test
    fun theZoomReadsAsAPercentageOfActualSize() = runComposeUiTest {
        val viewport = viewport()
        setContent { PreviewToolbar(viewport, isBackdropDark = false) }
        onNodeWithText("50%").assertExists()
        onNodeWithContentDescription("Zoom in").performClick()
        onNodeWithText("63%").assertExists()
    }

    @Test
    fun steppingStopsAtActualSizeOnTheWayPast() = runComposeUiTest {
        val viewport = viewport()
        setContent { PreviewToolbar(viewport, isBackdropDark = false) }
        repeat(4) { onNodeWithContentDescription("Zoom in").performClick() }
        // 1.25, 1.56, 1.95, then the step that would pass 2 lands on it.
        assertEquals(2f, viewport.zoom)
        onNodeWithText("100%").assertExists()

        onNodeWithContentDescription("Zoom in").performClick()
        assertEquals(2.5f, viewport.zoom)
        onNodeWithContentDescription("Zoom out").performClick()
        assertEquals(2f, viewport.zoom)
    }

    @Test
    fun theReadoutJumpsToActualSize() = runComposeUiTest {
        val viewport = viewport()
        setContent { PreviewToolbar(viewport, isBackdropDark = false) }
        onNodeWithText("50%").performClick()
        assertEquals(2f, viewport.zoom)
    }

    @Test
    fun fittingIsOnlyAvailableWhileZoomed() = runComposeUiTest {
        val viewport = viewport()
        setContent { PreviewToolbar(viewport, isBackdropDark = true) }
        onNodeWithContentDescription("Fit to window").assertIsNotEnabled()

        onNodeWithContentDescription("Zoom out").performClick()
        onNodeWithContentDescription("Fit to window").assertIsEnabled().performClick()
        assertEquals(1f, viewport.zoom)
        onNodeWithContentDescription("Fit to window").assertIsNotEnabled()
    }

    @Test
    fun dragToScrollIsASwitchThatTurnsItOnAndOff() = runComposeUiTest {
        var isDragToScroll by mutableStateOf(true)
        setContent {
            PreviewToolbar(
                viewport(),
                isBackdropDark = false,
                isDragToScroll = isDragToScroll,
                onDragToScrollChange = { isDragToScroll = it },
            )
        }
        onNodeWithContentDescription("Drag to scroll").assertIsOn().performClick()
        assertFalse(isDragToScroll)
        onNodeWithContentDescription("Drag to scroll").assertIsOff().performClick()
        assertTrue(isDragToScroll)
    }

    @Test
    fun thereIsNoDragToScrollSwitchWhereTheMouseCannotActAsAFinger() = runComposeUiTest {
        setContent { PreviewToolbar(viewport(), isBackdropDark = false) }
        onNodeWithContentDescription("Drag to scroll").assertDoesNotExist()
    }

    @Test
    fun turningDragToScrollOffHoldsForTheNextDevice() = runComposeUiTest {
        var device by mutableStateOf<Device>(DevicePresets.pixel9)
        setContent { PreviewWithDragToScroll(device) }
        onNodeWithContentDescription("Drag to scroll").assertIsOn().performClick()

        device = DevicePresets.galaxyTabS11
        onNodeWithContentDescription("Drag to scroll").assertIsOff()
    }

    @Test
    fun turningDragToScrollBackToTheDefaultLetsEachDeviceChoose() = runComposeUiTest {
        var device by mutableStateOf<Device>(DevicePresets.pixel9)
        setContent { PreviewWithDragToScroll(device) }
        onNodeWithContentDescription("Drag to scroll").performClick().performClick()

        device = DevicePresets.largeDesktopWindow
        onNodeWithContentDescription("Drag to scroll").assertIsOff()
        device = DevicePresets.pixel9
        onNodeWithContentDescription("Drag to scroll").assertIsOn()
    }

    @Test
    fun theScreenshotMenuWaitsForADeviceToBeShown() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalDeviceScreenshots provides DeviceScreenshots()) {
                ScreenshotMenu(DevicePresets.pixel9, isBackdropDark = false, download = { _, _ -> })
            }
        }
        onNodeWithContentDescription("Screenshot").assertIsNotEnabled()
    }

    @Test
    fun thereIsNoScreenshotMenuWithoutAnythingToRecordScreenshots() = runComposeUiTest {
        setContent { ScreenshotMenu(DevicePresets.pixel9, isBackdropDark = false, download = { _, _ -> }) }
        onNodeWithContentDescription("Screenshot").assertDoesNotExist()
    }

    @Test
    fun theScreenshotMenuSavesOrCopiesTheScreenOrTheWholeDevice() = runComposeUiTest {
        val saved = mutableListOf<String>()
        val copied = mutableListOf<ImageBitmap>()
        setContent {
            DeviceWithScreenshotMenu(download = { _, fileName -> saved += fileName }) { image, onDone ->
                copied += image
                onDone(true)
            }
        }
        onNodeWithContentDescription("Screenshot").performClick()
        onNodeWithText("Save with frame").performClick()
        assertEquals(listOf("Pixel 9 with frame.png"), saved)

        onNodeWithContentDescription("Screenshot").performClick()
        onNodeWithText("Copy screen").performClick()
        // The screen alone, at the device's own resolution.
        assertEquals(listOf(1080 to 2424), copied.map { it.width to it.height })
        onNodeWithText("Copied").assertExists()
    }

    @Test
    fun aCopyTheClipboardRefusesSaysSo() = runComposeUiTest {
        setContent { DeviceWithScreenshotMenu(download = { _, _ -> }) { _, onDone -> onDone(false) } }
        onNodeWithContentDescription("Screenshot").performClick()
        onNodeWithText("Copy with frame").performClick()
        onNodeWithText("Couldn't copy").assertExists()
    }

    @Test
    fun aSecondCopyKeepsItsNoticeUpForTheFullTime() = runComposeUiTest {
        setContent { DeviceWithScreenshotMenu(download = { _, _ -> }) { _, onDone -> onDone(true) } }
        mainClock.autoAdvance = false
        repeat(2) {
            onNodeWithContentDescription("Screenshot").performClick()
            mainClock.advanceTimeBy(500)
            onNodeWithText("Copy screen").performClick()
            mainClock.advanceTimeBy(1000)
        }
        // The first notice would have gone 2 seconds after it came, half a second ago.
        onNodeWithText("Copied").assertExists()
        mainClock.advanceTimeBy(1100)
        onNodeWithText("Copied").assertDoesNotExist()
    }

    @Test
    fun whereScreenshotsCannotBeCopiedTheMenuOnlySavesThem() = runComposeUiTest {
        setContent { DeviceWithScreenshotMenu(download = { _, _ -> }, copy = null) }
        onNodeWithContentDescription("Screenshot").performClick()
        onNodeWithText("Save screen").assertExists()
        onNodeWithText("Copy screen").assertDoesNotExist()
    }

    @Composable
    private fun PreviewWithDragToScroll(device: Device) {
        ZoomableDevicePreview(SimulatedDevice(device), PreviewSettings(), onResize = null, isZoomable = true, canDragToScroll = true) {}
    }

    @Composable
    private fun DeviceWithScreenshotMenu(download: PngDownloader, copy: PngCopier?) {
        CompositionLocalProvider(LocalDeviceScreenshots provides remember { DeviceScreenshots() }) {
            Box(Modifier.size(400.dp)) {
                DeviceFrameView(DevicePresets.pixel9, Modifier.fillMaxSize()) {}
                ScreenshotMenu(DevicePresets.pixel9, isBackdropDark = false, Modifier.align(Alignment.BottomEnd), download, copy)
            }
        }
    }
}
