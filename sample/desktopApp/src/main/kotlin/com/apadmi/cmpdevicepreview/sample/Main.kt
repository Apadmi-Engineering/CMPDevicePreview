package com.apadmi.cmpdevicepreview.sample

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.apadmi.cmpdevicepreview.preview.DeviceWithConfigurationView

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CmpDevicePreview Sample",
        // Wide enough to show the configuration panel beside the device.
        state = rememberWindowState(size = DpSize(1280.dp, 900.dp)),
    ) {
        // The app runs inside the device preview; take this wrapper away and it runs on its own.
        DeviceWithConfigurationView(panel = samplePanel, languages = sampleLanguages) {
            App()
        }
    }
}
