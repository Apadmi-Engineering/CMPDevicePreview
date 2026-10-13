package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DeviceWithConfigurationViewTest {
    private val wide = 1000.dp
    private val narrow = 600.dp

    @Test
    fun thePanelIsBesideTheDeviceWhenThereIsRoomAndBehindAButtonIfNot() = runComposeUiTest {
        var width by mutableStateOf(wide)
        setContent {
            Box(Modifier.width(width).height(800.dp)) {
                DeviceWithConfigurationView(initialDevice = DevicePresets.iPhone16Pro) {}
            }
        }
        onNodeWithText("Device Preview").assertExists()

        width = narrow
        waitForIdle()
        onNodeWithText("Device Preview").assertDoesNotExist()
        onNode(hasText("iPhone 16 Pro") and hasClickAction()).performClick()
        waitForIdle()
        onNodeWithText("Device Preview").assertExists()
    }

    @Test
    fun crossingTheBreakpointKeepsTheSettingsAndTheAppsState() = runComposeUiTest {
        var width by mutableStateOf(wide)
        val appStates = mutableListOf<Any>()
        setContent {
            Box(Modifier.width(width).height(800.dp)) {
                DeviceWithConfigurationView {
                    appStates += remember { Any() }
                }
            }
        }
        onNodeWithText("Landscape").performClick()
        waitForIdle()

        for (step in listOf<Dp>(narrow, wide)) {
            width = step
            waitForIdle()
        }
        onNode(hasText("Landscape") and isSelectable()).assertIsSelected()
        assertEquals(1, appStates.distinct().size, "the app was composed afresh")
    }

    @Test
    fun startsWithTheGivenDeviceAndConfiguration() = runComposeUiTest {
        setContent {
            Box(Modifier.width(wide).height(800.dp)) {
                DeviceWithConfigurationView(
                    initialDevice = DevicePresets.pixel9,
                    initialConfiguration = DeviceConfiguration(isDarkMode = true),
                ) {}
            }
        }
        onNodeWithText("Pixel 9").assertExists()
        // The appearance's "Dark", before the backdrop's.
        onAllNodes(hasText("Dark") and isSelectable())[0].assertIsSelected()
    }
}
