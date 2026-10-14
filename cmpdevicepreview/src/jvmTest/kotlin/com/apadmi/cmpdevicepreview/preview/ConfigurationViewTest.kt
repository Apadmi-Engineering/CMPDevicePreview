package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

import com.apadmi.cmpdevicepreview.core.Backdrop
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.devices.DevicePresets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class ConfigurationViewTest {
    @Test
    fun yourOwnItemsAreShownInOrderAsWideAsTheSettings() {
        val shown = mutableListOf<String>()
        var width = -1
        var primary = Color.Unspecified
        renderComposeScene(300, 4000) {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                panelItems = listOf({
                    shown += "first"
                    primary = MaterialTheme.colorScheme.primary
                    Box(Modifier.fillMaxWidth().onSizeChanged { width = it.width })
                },
                    { shown += "second" },
                ),
            )
        }
        assertEquals(listOf("first", "second"), shown)
        // The panel's 16 dp margins, as its settings have.
        assertEquals(300 - 2 * 16, width)
        assertEquals(PanelColors.Accent, primary, "inside the panel's theme")
    }

    @Test
    fun aTypedSizeIsFittedToTheDevice() = runComposeUiTest {
        var changed: SimulatedDevice? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(FreeformDevice(Size(800f, 800f)), DeviceConfiguration(displayScale = 1.5f)),
                onSimulatedDeviceChange = { changed = it },
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        val width = onAllNodes(hasSetTextAction())[0]
        width.performTextReplacement("300")
        width.performImeAction()

        val resized = assertNotNull(changed)
        assertEquals(FreeformDevice(Size(300f, 800f)), resized.device)
        // A window 300 dp across offers no display size larger than the default.
        assertEquals(1f, resized.configuration.displayScale)
    }

    @Test
    fun theBackdropChangesThePreviewSettingsNotTheDevice() = runComposeUiTest {
        var device: SimulatedDevice? = null
        var settings: PreviewSettings? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = { device = it },
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = { settings = it },
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        onNode(hasText("Auto") and isSelectable()).performScrollTo().performClick()
        assertEquals(PreviewSettings(backdrop = Backdrop.AUTO), settings)

        onNodeWithText("Dot grid").performScrollTo().performClick()
        assertEquals(PreviewSettings(isDotGridVisible = false), settings)
        assertNull(device)
    }

    @Test
    fun theResetButtonsReturnTheTextAndDisplaySizesToTheirDefaults() = runComposeUiTest {
        var changed: SimulatedDevice? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(
                    DevicePresets.pixel9,
                    DeviceConfiguration(fontScale = 1.5f, displayScale = 0.85f),
                ),
                onSimulatedDeviceChange = { changed = it },
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        onNodeWithContentDescription("Reset Text size").performScrollTo().assertIsEnabled().performClick()
        assertEquals(DeviceConfiguration(fontScale = 1f, displayScale = 0.85f), assertNotNull(changed).configuration)

        onNodeWithContentDescription("Reset Display size").performScrollTo().assertIsEnabled().performClick()
        assertEquals(DeviceConfiguration(fontScale = 1.5f, displayScale = 1f), assertNotNull(changed).configuration)
    }

    @Test
    fun theResetButtonsAreOutOfTheWayWhileTheSizesAreTheDefaults() = runComposeUiTest {
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        onNodeWithContentDescription("Reset Text size").assertIsNotEnabled()
        onNodeWithContentDescription("Reset Display size").assertIsNotEnabled()
        onNodeWithContentDescription("Reset Layout direction").assertIsNotEnabled()
        onNodeWithContentDescription("Reset all settings").assertIsNotEnabled()
    }

    @Test
    fun resetAllReturnsEveryDeviceSettingToItsDefaultAndKeepsTheDevice() = runComposeUiTest {
        var changed: SimulatedDevice? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(
                    DevicePresets.pixel9,
                    DeviceConfiguration(
                        isPortrait = false,
                        isDarkMode = true,
                        isFrameVisible = false,
                        fontScale = 1.5f,
                        displayScale = 0.85f,
                        layoutDirection = LayoutDirection.Rtl,
                    ),
                ),
                onSimulatedDeviceChange = { changed = it },
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        onNodeWithContentDescription("Reset all settings").assertIsEnabled().performClick()
        assertEquals(SimulatedDevice(DevicePresets.pixel9), changed)
    }

    @Test
    fun theLayoutDirectionResetFollowsTheHostAgain() = runComposeUiTest {
        var changed: SimulatedDevice? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(layoutDirection = LayoutDirection.Rtl)),
                onSimulatedDeviceChange = { changed = it },
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        onNodeWithContentDescription("Reset Layout direction").performScrollTo().assertIsEnabled().performClick()
        assertNull(assertNotNull(changed).configuration.layoutDirection)
    }

    @Test
    fun theSlidersAreNamedAndAnnounceTheirValue() = runComposeUiTest {
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(fontScale = 1.5f)),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        val value = { text: String -> SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text) }
        onNodeWithContentDescription("Text size").performScrollTo().assert(value("150%"))
        onNodeWithContentDescription("Display size").performScrollTo().assert(value("Default"))
    }
}
