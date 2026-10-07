package com.kappmaker.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kappmaker.devicepreview.preview.DeviceWithConfigurationView
import com.kappmaker.devicepreview.preview.SimulatedDeviceSafeArea
import com.kappmaker.devicepreview.preview.SimulatedDeviceThemeIsDark
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(size: WindowSizeClass) {
    DeviceWithConfigurationView(size = size) {
        val isDark by SimulatedDeviceThemeIsDark.current //Should be replaced with a real theme change
        MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                // Keeps the first and last items clear of the notch / Dynamic Island and home
                // indicator, and of the simulated keyboard while it is raised.
                contentPadding = SimulatedDeviceSafeArea.current.asPaddingValues()
            ) {
                item {
                    // Focusing the field can't raise the simulated keyboard: use the Keyboard
                    // switch in the configuration panel.
                    var text by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Type here") },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
                items(50) {
                    Text(
                        "Item #$it",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }

}

