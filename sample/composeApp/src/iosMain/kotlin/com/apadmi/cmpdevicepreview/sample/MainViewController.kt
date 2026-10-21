package com.apadmi.cmpdevicepreview.sample

import androidx.compose.ui.window.ComposeUIViewController
import com.apadmi.cmpdevicepreview.preview.DeviceWithConfigurationView

fun MainViewController() = ComposeUIViewController {
    // The app runs inside the device preview; take this wrapper away and it runs on its own.
    DeviceWithConfigurationView(languages = sampleLanguages) {
        App()
    }
}
