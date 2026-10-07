package com.kappmaker.devicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kappmaker.devicepreview.core.DeviceConfiguration
import com.kappmaker.devicepreview.core.SimulatedDevice
import com.kappmaker.devicepreview.devices.DevicePresets

public val SimulatedDeviceThemeIsDark: ProvidableCompositionLocal<MutableState<Boolean>> =
    compositionLocalOf { mutableStateOf(false) }

/** How much of the space, in each direction, the device may fill: the rest is margin. */
private const val DeviceFraction = 0.9f

@Composable
public fun SimulatedDevicePreview(
    simulatedDevice: SimulatedDevice = SimulatedDevice(
        device = DevicePresets.pixel9,
        configuration = DeviceConfiguration(isPortrait = true, isDarkMode = false)
    ),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    val isDarkState =
        remember(simulatedDevice.configuration.isDarkMode) { mutableStateOf(simulatedDevice.configuration.isDarkMode) }

    CompositionLocalProvider(SimulatedDeviceThemeIsDark provides isDarkState) {
        val configuration = simulatedDevice.configuration
        Box(modifier = modifier.fillMaxSize().dotGrid(), contentAlignment = Alignment.Center) {
            DeviceFrameView(
                device = simulatedDevice.device,
                modifier = Modifier.padding(16.dp).fillMaxSize(DeviceFraction),
                isPortrait = configuration.isPortrait,
                isFrameVisible = configuration.isFrameVisible,
                isSystemUiVisible = configuration.isSystemUiVisible,
                isKeyboardVisible = configuration.isKeyboardVisible,
                // The content may flip the theme itself; the status bar icons follow it.
                isDarkMode = isDarkState.value,
                content = content,
            )
        }
    }

}
