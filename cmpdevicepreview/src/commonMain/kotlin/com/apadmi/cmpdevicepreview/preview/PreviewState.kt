package com.apadmi.cmpdevicepreview.preview

import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewLanguage
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice

/**
 * What the URL and the stored settings keep: the device with its configuration, the preview's
 * settings, and the values of the panel's own settings.
 *
 * @property simulatedDevice The device and how it is shown.
 * @property previewSettings How the preview around the device is shown.
 * @property settingValues The values of the settings of your own in the panel.
 */
internal data class PreviewState(
    val simulatedDevice: SimulatedDevice,
    val previewSettings: PreviewSettings,
    val settingValues: PanelSettingValues = PanelSettingValues(),
) {
    // Wherever the state came from, a link, the kept settings or a change of panel: what a hidden control sets
    // stays as the host started it, so nothing can be set that the panel has no control to undo.
    fun fittedTo(
        panel: PanelSpec,
        languages: List<PreviewLanguage>,
        initial: PreviewState
    ): PreviewState {
        val initialDevice = initial.simulatedDevice
        val configuration = simulatedDevice.configuration.lockedBy(panel, initialDevice.configuration)
        val steps = panel.textSizeSteps
        val fitted = SimulatedDevice(
            simulatedDevice.device.lockedBy(panel, initialDevice.device),
            configuration.copy(fontScale = configuration.fontScale.coerceIn(steps.first().scale, steps.last().scale)),
        )
        return PreviewState(
            fitted.adaptedToDevice().inLanguages(languages),
            previewSettings.lockedBy(panel, initial.previewSettings),
            settingValues.fittedTo(panel.settings),
        )
    }
}

private fun <T> PanelSpec.unlessHidden(
    control: PanelControl,
    value: T,
    initial: T
): T = if (control in controls) value else initial

// The corners resize a freeform window whether or not the picker is shown, so only which device it is stays.
private fun Device.lockedBy(panel: PanelSpec, initial: Device): Device =
    if (this is FreeformDevice && initial is FreeformDevice) this else panel.unlessHidden(PanelControl.Frame, this, initial)

private fun DeviceConfiguration.lockedBy(panel: PanelSpec, initial: DeviceConfiguration) = DeviceConfiguration(
    orientation = panel.unlessHidden(PanelControl.Orientation, orientation, initial.orientation),
    appearance = panel.unlessHidden(PanelControl.Appearance, appearance, initial.appearance),
    isFrameVisible = panel.unlessHidden(PanelControl.Bezel, isFrameVisible, initial.isFrameVisible),
    isSystemUiVisible = panel.unlessHidden(PanelControl.SystemUi, isSystemUiVisible, initial.isSystemUiVisible),
    isKeyboardVisible = panel.unlessHidden(PanelControl.Keyboard, isKeyboardVisible, initial.isKeyboardVisible),
    fontScale = panel.unlessHidden(PanelControl.TextSize, fontScale, initial.fontScale),
    displayScale = panel.unlessHidden(PanelControl.DisplaySize, displayScale, initial.displayScale),
    layoutDirection = panel.unlessHidden(PanelControl.LayoutDirection, layoutDirection, initial.layoutDirection),
    language = panel.unlessHidden(PanelControl.Language, language, initial.language),
)

private fun PreviewSettings.lockedBy(panel: PanelSpec, initial: PreviewSettings) = PreviewSettings(
    backdrop = panel.unlessHidden(PanelControl.Backdrop, backdrop, initial.backdrop),
    isDotGridVisible = panel.unlessHidden(PanelControl.DotGrid, isDotGridVisible, initial.isDotGridVisible),
    isPreviewModeEnabled = panel.unlessHidden(PanelControl.PreviewMode, isPreviewModeEnabled, initial.isPreviewModeEnabled),
)
