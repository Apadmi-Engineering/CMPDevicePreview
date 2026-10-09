package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apadmi.cmpdevicepreview.core.Backdrop
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.DevicePreset
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.core.adaptedToDevice
import com.apadmi.cmpdevicepreview.devices.DevicePresets

/** The device the preview starts with. */
internal val DefaultDevice: DevicePreset get() = DevicePresets.galaxyS25

/** How much of the space, in each direction, the device may fill: the rest is margin. */
private const val DeviceFraction = 0.9f

/**
 * Shows [simulatedDevice] with [content] inside it, on the backdrop its configuration asks for.
 *
 * @param onChangeConfiguration When set, the device can be resized by dragging its corners, and
 * this gets the resized device to pass back as [simulatedDevice].
 */
@Composable
public fun SimulatedDevicePreview(
    simulatedDevice: SimulatedDevice = SimulatedDevice(DefaultDevice),
    modifier: Modifier = Modifier,
    onChangeConfiguration: ((SimulatedDevice) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val configuration = simulatedDevice.configuration
    Box(modifier = modifier.fillMaxSize().backdrop(configuration), contentAlignment = Alignment.Center) {
        DeviceFrameView(
            device = simulatedDevice.device,
            modifier = Modifier.padding(16.dp).fillMaxSize(DeviceFraction),
            configuration = configuration,
            onResize = onChangeConfiguration?.let { change ->
                { freeform -> change(simulatedDevice.copy(device = freeform).adaptedToDevice()) }
            },
            content = content,
        )
    }
}

/** The backdrop [configuration] asks for: light or dark, with or without dots. */
private fun Modifier.backdrop(configuration: DeviceConfiguration): Modifier {
    val isDark = when (configuration.backdrop) {
        Backdrop.LIGHT -> false
        Backdrop.DARK -> true
        Backdrop.AUTO -> configuration.isDarkMode
    }
    val background = if (isDark) DotGridDefaults.DarkBackground else DotGridDefaults.Background
    return if (configuration.isDotGridVisible) {
        dotGrid(background = background, dot = if (isDark) DotGridDefaults.DarkDot else DotGridDefaults.Dot)
    } else {
        background(background)
    }
}
