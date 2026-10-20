package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals

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
        setContent { PreviewToolbar(viewport, DevicePresets.pixel9, isBackdropDark = false) }
        onNodeWithText("50%").assertExists()
        onNodeWithContentDescription("Zoom in").performClick()
        onNodeWithText("63%").assertExists()
    }

    @Test
    fun steppingStopsAtActualSizeOnTheWayPast() = runComposeUiTest {
        val viewport = viewport()
        setContent { PreviewToolbar(viewport, DevicePresets.pixel9, isBackdropDark = false) }
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
        setContent { PreviewToolbar(viewport, DevicePresets.pixel9, isBackdropDark = false) }
        onNodeWithText("50%").performClick()
        assertEquals(2f, viewport.zoom)
    }

    @Test
    fun fittingIsOnlyAvailableWhileZoomed() = runComposeUiTest {
        val viewport = viewport()
        setContent { PreviewToolbar(viewport, DevicePresets.pixel9, isBackdropDark = true) }
        onNodeWithContentDescription("Fit to window").assertIsNotEnabled()

        onNodeWithContentDescription("Zoom out").performClick()
        onNodeWithContentDescription("Fit to window").assertIsEnabled().performClick()
        assertEquals(1f, viewport.zoom)
        onNodeWithContentDescription("Fit to window").assertIsNotEnabled()
    }
}
