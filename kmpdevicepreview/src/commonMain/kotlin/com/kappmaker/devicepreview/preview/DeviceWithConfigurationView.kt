package com.kappmaker.devicepreview.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kappmaker.devicepreview.core.SimulatedDevice
import com.kappmaker.devicepreview.devices.DevicePresets
import com.kappmaker.devicepreview.getSourceSetName


@Composable
public fun DeviceWithConfigurationViewExpanded(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var simulatedDevice by remember { mutableStateOf(SimulatedDevice(device = DevicePresets.pixel9)) }
    if (simulatedDevice.configuration.isPreviewModeEnabled.not()) {
        RealDeviceContent(content)
        return
    }
    Row(modifier = modifier) {
        val contentModifier = Modifier
            .weight(1f)
            .fillMaxHeight()

        SimulatedDevicePreview(
            simulatedDevice = simulatedDevice,
            modifier = contentModifier,
            content = content
        )
        ConfigurationView(
            modifier = Modifier.fillMaxHeight().width(PanelWidth),
            simulatedDevice = simulatedDevice,
            onChangeConfiguration = {
                simulatedDevice = it
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun DeviceWithConfigurationViewCompact(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var simulatedDevice by remember { mutableStateOf(SimulatedDevice(device = DevicePresets.pixel9)) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    if (showBottomSheet) {
        ModalBottomSheet(
            dragHandle = { BottomSheetDefaults.DragHandle(color = PanelColors.SwitchOff) },
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState,
            containerColor = PanelColors.Background,
        ) {
            // Sheet content
            ConfigurationView(
                modifier = Modifier.fillMaxSize(),
                simulatedDevice = simulatedDevice,
                onChangeConfiguration = { newSimulatedDevice ->
                    simulatedDevice = newSimulatedDevice
                })
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (simulatedDevice.configuration.isPreviewModeEnabled.not()) {
            RealDeviceContent(content)
            return
        }
        val contentModifier = Modifier.weight(1f)
        SimulatedDevicePreview(
            modifier = contentModifier,
            simulatedDevice = simulatedDevice,
            content = content
        )

        Button(
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PanelColors.Background,
                contentColor = PanelColors.TextPrimary,
            ),
            onClick = { showBottomSheet = true })
        {
            Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(simulatedDevice.device.name, modifier = Modifier.padding(start = 10.dp), fontWeight = FontWeight.Medium)
        }

    }


}

@Composable
public fun DeviceWithConfigurationView(
    modifier: Modifier = Modifier,
    size: WindowSizeClass,
    content: @Composable () -> Unit
) {
    when (size.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> DeviceWithConfigurationViewExpanded(
            modifier = modifier,
            content = content
        )

        else -> DeviceWithConfigurationViewCompact(
            modifier = modifier,
            content = content
        )
    }
}

@Composable
public fun DeviceWithConfigurationView(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    when (getSourceSetName()) {
        "androidMain", "iosMain" -> DeviceWithConfigurationViewCompact(
            modifier = modifier,
            content = content
        )

        else -> DeviceWithConfigurationViewExpanded(
            modifier = modifier,
            content = content
        )
    }
}

/** The content on the real device, with the real device's safe area and keyboard. */
@Composable
private fun RealDeviceContent(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        SimulatedDeviceSafeArea provides WindowInsets.safeDrawing,
        SimulatedDeviceKeyboardInsets provides WindowInsets.ime,
    ) {
        content()
    }
}
