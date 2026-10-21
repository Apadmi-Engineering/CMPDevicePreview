package com.apadmi.cmpdevicepreview.sample

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import com.apadmi.cmpdevicepreview.preview.DeviceWithConfigurationView
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationActivationPolicy
import platform.AppKit.NSApplicationDelegateProtocol
import platform.darwin.NSObject

// Compose's native macOS Window opens a window but doesn't run the application, so this does.
// The application holds its delegate weakly, so it is kept here.
private val quitWhenClosed = object : NSObject(), NSApplicationDelegateProtocol {
    override fun applicationShouldTerminateAfterLastWindowClosed(sender: NSApplication): Boolean = true
}

fun main() {
    val application = NSApplication.sharedApplication()
    application.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
    application.delegate = quitWhenClosed
    Window(
        title = "CmpDevicePreview Sample",
        // Wide enough to show the configuration panel beside the device.
        size = DpSize(1280.dp, 900.dp),
    ) {
        // The app runs inside the device preview; take this wrapper away and it runs on its own.
        DeviceWithConfigurationView(panel = samplePanel, languages = sampleLanguages) {
            App()
        }
    }
    // Not `activate()`, which needs macOS 14.
    @Suppress("DEPRECATION")
    application.activateIgnoringOtherApps(true)
    application.run()
}
