package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apadmi.cmpdevicepreview.core.Appearance
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DeviceWithConfigurationViewTest {
    private val wide = 1000.dp
    private val narrow = 600.dp
    private val attribution = "Built with CMPDevicePreview"

    @Test
    fun thePanelIsBesideTheDeviceWhenThereIsRoomAndBehindAButtonIfNot() = runComposeUiTest {
        var width by mutableStateOf(wide)
        setContent {
            Box(Modifier.width(width).height(800.dp)) {
                DeviceWithConfigurationView(initialDevice = DevicePresets.iPhone16Pro, isPersistenceEnabled = false) {}
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
    fun theDeviceCanBeScreenshottedOnceItIsShown() = runComposeUiTest {
        setContent {
            Box(Modifier.width(wide).height(800.dp)) {
                DeviceWithConfigurationView(initialDevice = DevicePresets.iPhone16Pro, isPersistenceEnabled = false) {}
            }
        }
        waitForIdle()
        onNodeWithContentDescription("Screenshot of screen").assertIsEnabled()
        onNodeWithContentDescription("Screenshot with frame").assertIsEnabled()
    }

    @Test
    fun yourOwnTitleAndIconAreShownBesideTheDeviceAndInTheSheet() = runComposeUiTest {
        var width by mutableStateOf(wide)
        setContent {
            Box(Modifier.width(width).height(800.dp)) {
                DeviceWithConfigurationView(
                    initialDevice = DevicePresets.iPhone16Pro,
                    isPersistenceEnabled = false,
                    panelTitle = "Acme Preview",
                    panelIcon = { Box(Modifier.fillMaxSize().testTag("logo")) },
                ) {}
            }
        }
        onNodeWithText("Acme Preview").assertExists()
        onNodeWithTag("logo").assertExists()
        onNodeWithText("Device Preview").assertDoesNotExist()

        width = narrow
        waitForIdle()
        onNodeWithTag("logo").assertDoesNotExist()
        onNode(hasText("iPhone 16 Pro") and hasClickAction()).performClick()
        waitForIdle()
        onNodeWithText("Acme Preview").assertExists()
        onNodeWithTag("logo").assertExists()
        onNodeWithText("Device Preview").assertDoesNotExist()
    }

    @Test
    fun theLinkToTheProjectCanBeHiddenBesideTheDeviceAndInTheSheet() = runComposeUiTest {
        var width by mutableStateOf(wide)
        var isAttributionVisible by mutableStateOf(true)
        setContent {
            Box(Modifier.width(width).height(800.dp)) {
                DeviceWithConfigurationView(
                    initialDevice = DevicePresets.iPhone16Pro,
                    isPersistenceEnabled = false,
                    isAttributionVisible = isAttributionVisible,
                ) {}
            }
        }
        onNodeWithText(attribution).assertExists()

        width = narrow
        waitForIdle()
        onNode(hasText("iPhone 16 Pro") and hasClickAction()).performClick()
        waitForIdle()
        onNodeWithText(attribution).assertExists()

        isAttributionVisible = false
        waitForIdle()
        onNodeWithText(attribution).assertDoesNotExist()

        width = wide
        waitForIdle()
        onNodeWithText("Device Preview").assertExists()
        onNodeWithText(attribution).assertDoesNotExist()
    }

    @Test
    fun crossingTheBreakpointKeepsTheSettingsAndTheAppsState() = runComposeUiTest {
        var width by mutableStateOf(wide)
        val appStates = mutableListOf<Any>()
        setContent {
            Box(Modifier.width(width).height(800.dp)) {
                DeviceWithConfigurationView(isPersistenceEnabled = false) {
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
                    isPersistenceEnabled = false,
                    initialConfiguration = DeviceConfiguration(appearance = Appearance.Dark),
                ) {}
            }
        }
        onNodeWithText("Pixel 9").assertExists()
        // The appearance's "Dark", before the backdrop's.
        onAllNodes(hasText("Dark") and isSelectable())[0].assertIsSelected()
    }

    @Test
    fun aButtonTurnsPreviewModeBackOnAfterThePanelTurnsItOff() = runComposeUiTest {
        setContent {
            Box(Modifier.width(wide).height(800.dp)) {
                DeviceWithConfigurationView(isPersistenceEnabled = false) { Text("The app") }
            }
        }
        onNodeWithContentDescription(restoreButton).assertDoesNotExist()

        onNodeWithText("Preview mode").performClick()
        waitForIdle()
        onNodeWithText("Device Preview").assertDoesNotExist()
        onNodeWithText("The app").assertExists()

        onNodeWithContentDescription(restoreButton).performClick()
        waitForIdle()
        onNodeWithText("Device Preview").assertExists()
        onNodeWithText("The app").assertExists()
        onNodeWithContentDescription(restoreButton).assertDoesNotExist()
    }

    @Test
    fun startsWithJustTheAppAndTheButtonWhenPreviewModeStartsOff() = runComposeUiTest {
        setContent {
            Box(Modifier.width(wide).height(800.dp)) {
                DeviceWithConfigurationView(
                    initialPreviewSettings = PreviewSettings(isPreviewModeEnabled = false),
                    isPersistenceEnabled = false,
                ) {
                    Text("The app")
                }
            }
        }
        onNodeWithText("The app").assertExists()
        onNodeWithText("Device Preview").assertDoesNotExist()
        onNodeWithContentDescription(restoreButton).assertExists()
    }

    @Test
    fun theGlowAroundTheButtonLetsTheAppBelowItTakeClicks() = runComposeUiTest {
        var taps = 0
        setContent {
            Box(Modifier.width(wide).height(800.dp)) {
                DeviceWithConfigurationView(
                    initialPreviewSettings = PreviewSettings(isPreviewModeEnabled = false),
                    isPersistenceEnabled = false,
                ) {
                    Box(Modifier.fillMaxSize().clickable { taps++ })
                }
            }
        }
        // Inside the glow, to the left of the button.
        onRoot().performTouchInput { click(Offset(width - 80.dp.toPx(), height - 38.dp.toPx())) }
        assertEquals(1, taps)
        onNodeWithContentDescription(restoreButton).assertExists()
    }

    @Test
    fun theSettingsAreKeptWhilePreviewModeIsOff() = runComposeUiTest {
        setContent {
            Box(Modifier.width(wide).height(800.dp)) {
                DeviceWithConfigurationView(initialDevice = DevicePresets.pixel9, isPersistenceEnabled = false) {}
            }
        }
        onNodeWithText("Landscape").performClick()
        onNodeWithText("Preview mode").performClick()
        waitForIdle()
        onNodeWithContentDescription(restoreButton).performClick()
        waitForIdle()

        onNodeWithText("Pixel 9").assertExists()
        onNode(hasText("Landscape") and isSelectable()).assertIsSelected()
    }

    private companion object {
        const val restoreButton = "Turn preview mode on"
    }
}
