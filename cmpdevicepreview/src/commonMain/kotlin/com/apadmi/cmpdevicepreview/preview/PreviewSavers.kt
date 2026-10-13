package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import com.apadmi.cmpdevicepreview.core.Backdrop
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.DevicePreset
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.core.adaptedToDevice
import com.apadmi.cmpdevicepreview.devices.DevicePresets

/**
 * Saves a [SimulatedDevice] as values a platform can keep, such as in an Android `Bundle`. Built-in
 * presets and freeform windows are restored as they were; any other device can only be
 * [initialDevice], so that is what it restores as.
 */
internal fun simulatedDeviceSaver(initialDevice: Device): Saver<SimulatedDevice, Any> = mapSaver(
    save = { (device, configuration) ->
        buildMap {
            when {
                device == initialDevice -> Unit
                device is FreeformDevice -> {
                    put(FreeformWidth, device.size.width)
                    put(FreeformHeight, device.size.height)
                }
                device is DevicePreset && DevicePresets.byId(device.id) == device -> put(PresetId, device.id)
            }
            put(IsPortrait, configuration.isPortrait)
            put(IsDarkMode, configuration.isDarkMode)
            put(IsFrameVisible, configuration.isFrameVisible)
            put(IsSystemUiVisible, configuration.isSystemUiVisible)
            put(IsKeyboardVisible, configuration.isKeyboardVisible)
            put(FontScale, configuration.fontScale)
            put(DisplayScale, configuration.displayScale)
            put(LayoutDirectionName, configuration.layoutDirection?.name)
        }
    },
    restore = { saved ->
        val width = saved[FreeformWidth] as Float?
        val height = saved[FreeformHeight] as Float?
        val presetId = saved[PresetId] as String?
        val device = when {
            width != null && height != null -> FreeformDevice(Size(width, height))
            presetId != null -> DevicePresets.byId(presetId) ?: initialDevice
            else -> initialDevice
        }
        val configuration = DeviceConfiguration(
            isPortrait = saved[IsPortrait] as Boolean,
            isDarkMode = saved[IsDarkMode] as Boolean,
            isFrameVisible = saved[IsFrameVisible] as Boolean,
            isSystemUiVisible = saved[IsSystemUiVisible] as Boolean,
            isKeyboardVisible = saved[IsKeyboardVisible] as Boolean,
            fontScale = saved[FontScale] as Float,
            displayScale = saved[DisplayScale] as Float,
            layoutDirection = (saved[LayoutDirectionName] as String?)?.let(LayoutDirection::valueOf),
        )
        SimulatedDevice(device, configuration).adaptedToDevice()
    },
)

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

private const val FreeformWidth = "freeformWidth"
private const val FreeformHeight = "freeformHeight"
private const val PresetId = "presetId"
private const val IsPortrait = "isPortrait"
private const val IsDarkMode = "isDarkMode"
private const val IsFrameVisible = "isFrameVisible"
private const val IsSystemUiVisible = "isSystemUiVisible"
private const val IsKeyboardVisible = "isKeyboardVisible"
private const val FontScale = "fontScale"
private const val DisplayScale = "displayScale"
private const val LayoutDirectionName = "layoutDirection"
private const val BackdropName = "backdrop"
private const val IsDotGridVisible = "isDotGridVisible"
private const val IsPreviewModeEnabled = "isPreviewModeEnabled"
