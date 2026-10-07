package com.kappmaker.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kappmaker.devicepreview.preview.DeviceWithConfigurationView
import com.kappmaker.devicepreview.preview.SimulatedDeviceSafeArea
import com.kappmaker.devicepreview.preview.SimulatedDeviceThemeIsDark
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App(size: WindowSizeClass) {
    DeviceWithConfigurationView(size = size) {
        val isDark by SimulatedDeviceThemeIsDark.current //Should be replaced with a real theme change
        MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
            var background by remember { mutableStateOf(StatusBarBackground.None) }

            val safeArea = SimulatedDeviceSafeArea.current
            Scaffold(
                topBar = {
                    // An edge-to-edge app colours its status bar by drawing behind it: the app bar
                    // pads its title below the bar and fills the bar with its own colour.
                    background.color()?.let { color ->
                        TopAppBar(
                            title = { Text("Status bar") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = color,
                                titleContentColor = contentColorFor(color),
                            ),
                            // Material's default reads the real status bar, which the preview can't change.
                            windowInsets = safeArea.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        )
                    }
                },
                // Keeps the content clear of the notch / Dynamic Island and home indicator, and of
                // the simulated keyboard while it is raised.
                contentWindowInsets = safeArea,
            ) { padding ->
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
                    item {
                        StatusBarControls(background = background, onBackgroundChange = { background = it })
                    }
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

}

private enum class StatusBarBackground(val label: String) {
    /** No app bar: the content scrolls behind the transparent status bar. */
    None("None"),
    Surface("Surface"),
    Primary("Primary"),
    Inverse("Inverse"),
}

@Composable
private fun StatusBarBackground.color(): Color? = when (this) {
    StatusBarBackground.None -> null
    StatusBarBackground.Surface -> MaterialTheme.colorScheme.surface
    StatusBarBackground.Primary -> MaterialTheme.colorScheme.primary
    StatusBarBackground.Inverse -> MaterialTheme.colorScheme.inverseSurface
}

@Composable
private fun StatusBarControls(
    background: StatusBarBackground,
    onBackgroundChange: (StatusBarBackground) -> Unit,
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Status bar background", style = MaterialTheme.typography.titleSmall)
        Choices(StatusBarBackground.entries, background, onBackgroundChange) { it.label }
    }
}

@Composable
private fun <T> Choices(options: List<T>, selected: T, onSelect: (T) -> Unit, label: (T) -> String) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                // No check mark, so four labels fit on the narrowest phones.
                icon = {},
                label = { Text(label(option), maxLines = 1) },
            )
        }
    }
}
