package com.kappmaker.devicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kappmaker.devicepreview.core.DeviceConfiguration
import com.kappmaker.devicepreview.core.DevicePreset
import com.kappmaker.devicepreview.core.SimulatedDevice
import com.kappmaker.devicepreview.core.droppingUnavailableKeyboard
import com.kappmaker.devicepreview.devices.DevicePresets

/** The device the preview starts with. */
internal val DefaultDevice: DevicePreset get() = DevicePresets.galaxyS25

/** How much of the space, in each direction, the device may fill: the rest is margin. */
private const val DeviceFraction = 0.9f

/**
 * Shows [simulatedDevice] with [content] inside it, on a dotted backdrop.
 *
 * @param onChangeConfiguration When set, dragging a corner of the device resizes it into a
 * [com.kappmaker.devicepreview.core.FreeformDevice], and this is called with the changed
 * [SimulatedDevice] on release. Pass it back as [simulatedDevice] to apply it. When null, the
 * device has no resize handles.
 */
@Composable
public fun SimulatedDevicePreview(
    simulatedDevice: SimulatedDevice = SimulatedDevice(
        device = DefaultDevice,
        configuration = DeviceConfiguration(isPortrait = true, isDarkMode = false)
    ),
    modifier: Modifier = Modifier,
    onChangeConfiguration: ((SimulatedDevice) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val configuration = simulatedDevice.configuration
    Box(modifier = modifier.fillMaxSize().dotGrid(), contentAlignment = Alignment.Center) {
        DeviceFrameView(
            device = simulatedDevice.device,
            modifier = Modifier.padding(16.dp).fillMaxSize(DeviceFraction),
            isPortrait = configuration.isPortrait,
            isFrameVisible = configuration.isFrameVisible,
            isSystemUiVisible = configuration.isSystemUiVisible,
            isKeyboardVisible = configuration.isKeyboardVisible,
            isDarkMode = configuration.isDarkMode,
            onResize = onChangeConfiguration?.let { change ->
                { freeform -> change(simulatedDevice.copy(device = freeform).droppingUnavailableKeyboard()) }
            },
            content = content,
        )
    }
}
