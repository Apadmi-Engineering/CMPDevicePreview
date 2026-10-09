package com.apadmi.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.apadmi.cmpdevicepreview.preview.DeviceWithConfigurationView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // The app runs inside the device preview; take this wrapper away and it runs on its own.
            // Android can't override the window insets the app sees, so the preview sits inside the
            // real system bars, which also keeps them out of the app: only the simulated device's
            // status bar is drawn over it.
            DeviceWithConfigurationView(Modifier.safeDrawingPadding()) {
                App()
            }
        }
    }
}
