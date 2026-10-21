package com.apadmi.cmpdevicepreview.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.apadmi.cmpdevicepreview.preview.DeviceWithConfigurationView
import com.apadmi.cmpdevicepreview.sample.App
import com.apadmi.cmpdevicepreview.sample.sampleLanguages

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // The app runs inside the device preview; take this wrapper away and it runs on its own.
            // Android's insets can't be simulated, so the preview sits inside the real system bars.
            DeviceWithConfigurationView(Modifier.safeDrawingPadding(), languages = sampleLanguages) {
                App()
            }
        }
    }
}
