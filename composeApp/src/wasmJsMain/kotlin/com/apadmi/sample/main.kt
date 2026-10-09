package com.apadmi.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.apadmi.cmpdevicepreview.preview.DeviceWithConfigurationView
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        // The app runs inside the device preview; take this wrapper away and it runs on its own.
        DeviceWithConfigurationView {
            App()
        }
    }
}
