package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import com.apadmi.cmpdevicepreview.core.Appearance
import com.apadmi.cmpdevicepreview.core.Backdrop
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.DeviceOrientation
import com.apadmi.cmpdevicepreview.core.DevicePreset
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.devices.DevicePresets

private const val FreeformWidth = "freeformWidth"
private const val FreeformHeight = "freeformHeight"
private const val PresetId = "presetId"
private const val OrientationName = "orientation"
private const val AppearanceName = "appearance"
private const val IsFrameVisible = "isFrameVisible"
private const val IsSystemUiVisible = "isSystemUiVisible"
private const val IsKeyboardVisible = "isKeyboardVisible"
private const val FontScale = "fontScale"
private const val DisplayScale = "displayScale"
private const val LayoutDirectionName = "layoutDirection"
private const val LanguageName = "language"
private const val BackdropName = "backdrop"
private const val IsDotGridVisible = "isDotGridVisible"
private const val IsPreviewModeEnabled = "isPreviewModeEnabled"

internal val PreviewSettingsSaver: Saver<PreviewSettings, Any> = mapSaver(
    save = { settings ->
        mapOf(
            BackdropName to settings.backdrop.name,
            IsDotGridVisible to settings.isDotGridVisible,
            IsPreviewModeEnabled to settings.isPreviewModeEnabled,
        )
    },
    restore = { saved ->
        PreviewSettings(
            backdrop = Backdrop.valueOf(saved[BackdropName] as String),
            isDotGridVisible = saved[IsDotGridVisible] as Boolean,
            isPreviewModeEnabled = saved[IsPreviewModeEnabled] as Boolean,
        )
    },
)

/** This device and its configuration as saved values; [initialDevice] needs no more than the configuration. */
private fun SimulatedDevice.toSavedValues(initialDevice: Device): Map<String, Any?> = buildMap {
    when {
        device == initialDevice -> Unit
        device is FreeformDevice -> {
            put(FreeformWidth, device.size.width)
            put(FreeformHeight, device.size.height)
        }
        device is DevicePreset && DevicePresets.byId(device.id) == device -> put(PresetId, device.id)
        else -> {
            // this is a generated else block
        }
    }
    put(OrientationName, configuration.orientation.name)
    put(AppearanceName, configuration.appearance.name)
    put(IsFrameVisible, configuration.isFrameVisible)
    put(IsSystemUiVisible, configuration.isSystemUiVisible)
    put(IsKeyboardVisible, configuration.isKeyboardVisible)
    put(FontScale, configuration.fontScale)
    put(DisplayScale, configuration.displayScale)
    put(LayoutDirectionName, configuration.layoutDirection?.name)
    put(LanguageName, configuration.language)
}

/**
 * Saves a [SimulatedDevice] as values a platform can keep, such as in an Android `Bundle`. Built-in
 * presets and freeform windows are restored as they were; any other device can only be
 * [initialDevice], so that is what it restores as.
 *
 * @param initialDevice The device the view starts with: what a saved device that isn't a built-in
 * preset or a freeform window comes back as.
 * @return The saver for a [SimulatedDevice].
 */
internal fun simulatedDeviceSaver(initialDevice: Device): Saver<SimulatedDevice, Any> = mapSaver(
    save = { it.toSavedValues(initialDevice) },
    restore = { restoreSimulatedDevice(it, initialDevice) },
)

/** The [SimulatedDevice] that [saved] was saved from, with [initialDevice] for a device it can't restore. */
private fun restoreSimulatedDevice(saved: Map<String, Any?>, initialDevice: Device): SimulatedDevice {
    val width = saved[FreeformWidth] as Float?
    val height = saved[FreeformHeight] as Float?
    val presetId = saved[PresetId] as String?
    val device = when {
        width != null && height != null -> FreeformDevice(Size(width, height))
        presetId != null -> DevicePresets.byId(presetId) ?: initialDevice
        else -> initialDevice
    }
    val configuration = DeviceConfiguration(
        orientation = DeviceOrientation.valueOf(saved[OrientationName] as String),
        appearance = Appearance.valueOf(saved[AppearanceName] as String),
        isFrameVisible = saved[IsFrameVisible] as Boolean,
        isSystemUiVisible = saved[IsSystemUiVisible] as Boolean,
        isKeyboardVisible = saved[IsKeyboardVisible] as Boolean,
        fontScale = saved[FontScale] as Float,
        displayScale = saved[DisplayScale] as Float,
        layoutDirection = (saved[LayoutDirectionName] as String?)?.let(LayoutDirection::valueOf),
        language = saved[LanguageName] as String?,
    )
    return SimulatedDevice(device, configuration).adaptedToDevice()
}
