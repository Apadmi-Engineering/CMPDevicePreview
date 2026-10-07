package com.kappmaker.devicepreview.preview

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kappmaker.devicepreview.core.DeviceConfiguration
import com.kappmaker.devicepreview.core.SimulatedDevice
import com.kappmaker.devicepreview.devices.DevicePresets

public val SimulatedDeviceThemeIsDark: ProvidableCompositionLocal<MutableState<Boolean>> =
    compositionLocalOf { mutableStateOf(false) }


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
        DeviceFrameView(
            device = simulatedDevice.device,
            modifier = modifier.padding(20.dp),
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
