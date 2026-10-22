package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import com.apadmi.cmpdevicepreview.core.Appearance
import com.apadmi.cmpdevicepreview.core.Backdrop
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.DeviceOrientation
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewLanguage
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.core.displayScaleSteps
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private val Languages = listOf(PreviewLanguage("en", "English"), PreviewLanguage("cy", "Cymraeg"))

class PanelFittingTest {
    private val initial = PreviewState(SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(language = "en")), PreviewSettings())

    // Every setting away from the initial one, as the panel could leave them.
    private val changed = PreviewState(
        SimulatedDevice(
            DevicePresets.iPhone16Pro,
            DeviceConfiguration(
                orientation = DeviceOrientation.Landscape,
                appearance = Appearance.Dark,
                isFrameVisible = false,
                isSystemUiVisible = false,
                isKeyboardVisible = true,
                fontScale = 1.5f,
                displayScale = DevicePresets.iPhone16Pro.displayScaleSteps.last().scale,
                layoutDirection = LayoutDirection.Rtl,
                language = "cy",
            ),
        ),
        PreviewSettings(backdrop = Backdrop.Dark, isDotGridVisible = false, isPreviewModeEnabled = false),
        PanelSettingValues().with(MockApi, true),
    )

    private fun PreviewState.fittedTo(from: PreviewState = initial, panel: PanelScope.() -> Unit): PreviewState =
        fittedTo(buildPanel(panel), Languages, from)

    @Test
    fun everySettingIsKeptWhileItsControlIsShown() {
        val fitted = changed.fittedTo {
            defaults()
            setting(MockApi)
        }
        assertEquals(changed, fitted)
    }

    @Test
    fun aHiddenSettingIsLockedToItsInitialValue() {
        val locks = listOf(
            Lock("frame", { frame.isVisible = false }) { it.simulatedDevice.device },
            Lock("orientation", { orientation.isVisible = false }) { it.simulatedDevice.configuration.orientation },
            Lock("appearance", { appearance.isVisible = false }) { it.simulatedDevice.configuration.appearance },
            Lock("textSize", { textSize.isVisible = false }) { it.simulatedDevice.configuration.fontScale },
            Lock("displaySize", { displaySize.isVisible = false }) { it.simulatedDevice.configuration.displayScale },
            Lock("layoutDirection", { layoutDirection.isVisible = false }) { it.simulatedDevice.configuration.layoutDirection },
            Lock("language", { language.isVisible = false }) { it.simulatedDevice.configuration.language },
            Lock("bezel", { bezel.isVisible = false }) { it.simulatedDevice.configuration.isFrameVisible },
            Lock("systemUi", { systemUi.isVisible = false }) { it.simulatedDevice.configuration.isSystemUiVisible },
            Lock("keyboard", { keyboard.isVisible = false }) { it.simulatedDevice.configuration.isKeyboardVisible },
            Lock("backdrop", { backdrop.isVisible = false }) { it.previewSettings.backdrop },
            Lock("dotGrid", { dotGrid.isVisible = false }) { it.previewSettings.isDotGridVisible },
            Lock("previewMode", { previewMode.isVisible = false }) { it.previewSettings.isPreviewModeEnabled },
        )
        for (lock in locks) {
            assertNotEquals(lock.read(initial), lock.read(changed), lock.name)
            val fitted = changed.fittedTo { defaults(lock.hide) }
            assertEquals(lock.read(initial), lock.read(fitted), lock.name)
        }
    }

    @Test
    fun aHiddenFrameKeepsAFreeformWindowsSize() {
        val freeform = PreviewState(SimulatedDevice(FreeformDevice(Size(400f, 800f))), PreviewSettings())
        val resized = freeform.copy(simulatedDevice = SimulatedDevice(FreeformDevice(Size(600f, 900f))))
        assertEquals(resized.simulatedDevice.device, resized.fittedTo(freeform) { defaults { frame.isVisible = false } }.simulatedDevice.device)

        val picked = freeform.copy(simulatedDevice = SimulatedDevice(DevicePresets.pixel9))
        assertEquals(freeform.simulatedDevice.device, picked.fittedTo(freeform) { defaults { frame.isVisible = false } }.simulatedDevice.device)
    }

    @Test
    fun aTextSizeOutsideTheRangeIsMovedIntoIt() {
        val fontScaleIn = { scale: Float ->
            val state = initial.copy(simulatedDevice = initial.simulatedDevice.copy(configuration = DeviceConfiguration(fontScale = scale)))
            val fitted = state.fittedTo { textSize(range = 0.85f..2f) }
            fitted.simulatedDevice.configuration.fontScale
        }
        assertEquals(2f, fontScaleIn(3f))
        assertEquals(0.85f, fontScaleIn(0.5f))
        assertEquals(1.23f, fontScaleIn(1.23f))
    }

    @Test
    fun customValuesForSettingsNotInThePanelAreDropped() {
        val state = initial.copy(settingValues = PanelSettingValues(mapOf("mockApi" to "1", "env" to "Staging", "other" to "1")))
        assertEquals(PanelSettingValues().with(MockApi, true), state.fittedTo { setting(MockApi) }.settingValues)
    }

    @Test
    fun aChoiceValueIsKeptAsItsOptionIsWrittenAndDroppedWhenItIsNotOne() {
        val written = { value: String ->
            initial.copy(settingValues = PanelSettingValues(mapOf("env" to value))).fittedTo { setting(Environment) }.settingValues
        }
        assertEquals(PanelSettingValues().with(Environment, "Staging"), written("staging"))
        assertEquals(PanelSettingValues(), written("Prod"))
        // The default is what no value means, so it isn't kept.
        assertEquals(PanelSettingValues(), written("Dev"))
    }

    @Test
    fun aToggleIsReadAsAnyFormOfAFlag() {
        val written = { value: String ->
            initial.copy(settingValues = PanelSettingValues(mapOf("mockApi" to value))).fittedTo { setting(MockApi) }.settingValues
        }
        assertEquals(PanelSettingValues().with(MockApi, true), written("true"))
        assertEquals(PanelSettingValues().with(MockApi, true), written("ON"))
        assertEquals(PanelSettingValues(), written("0"))
        assertEquals(PanelSettingValues(), written("maybe"))
    }
}

/**
 * A control to hide, and the setting it locks.
 *
 * @property name The control, for a failure's message.
 * @property hide Hides the control in the usual panel.
 * @property read Reads the setting from a state.
 */
private class Lock(
    val name: String,
    val hide: DefaultPanelScope.() -> Unit,
    val read: (PreviewState) -> Any?,
)
