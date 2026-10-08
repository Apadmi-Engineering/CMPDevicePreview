package com.kappmaker.devicepreview.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.kappmaker.devicepreview.core.Device
import com.kappmaker.devicepreview.core.FreeformDevice
import com.kappmaker.devicepreview.core.SimulatedDevice
import com.kappmaker.devicepreview.getSourceSetName
import kotlin.math.roundToInt


@Composable
public fun DeviceWithConfigurationViewExpanded(
    modifier: Modifier = Modifier,
    panelItems: List<@Composable () -> Unit> = emptyList(),
    content: @Composable () -> Unit
) {
    var simulatedDevice by remember { mutableStateOf(SimulatedDevice(device = DefaultDevice)) }
    if (simulatedDevice.configuration.isPreviewModeEnabled.not()) {
        content()
        return
    }
    CompositionLocalProvider(LocalDeviceScreenshots provides rememberDeviceScreenshots()) {
        Row(modifier = modifier) {
            val contentModifier = Modifier
                .weight(1f)
                .fillMaxHeight()

            SimulatedDevicePreview(
                simulatedDevice = simulatedDevice,
                modifier = contentModifier,
                onChangeConfiguration = { simulatedDevice = it },
                content = content
            )
            ConfigurationView(
                modifier = Modifier.fillMaxHeight().width(PanelWidth),
                simulatedDevice = simulatedDevice,
                onChangeConfiguration = {
                    simulatedDevice = it
                },
                panelItems = panelItems,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun DeviceWithConfigurationViewCompact(
    modifier: Modifier = Modifier,
    panelItems: List<@Composable () -> Unit> = emptyList(),
    content: @Composable () -> Unit
) {
    var simulatedDevice by remember { mutableStateOf(SimulatedDevice(device = DefaultDevice)) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalDeviceScreenshots provides rememberDeviceScreenshots()) {
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
                    },
                    panelItems = panelItems,
                )
            }
        }

        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            if (simulatedDevice.configuration.isPreviewModeEnabled.not()) {
                content()
                return@Column
            }
            val contentModifier = Modifier.weight(1f)
            SimulatedDevicePreview(
                modifier = contentModifier,
                simulatedDevice = simulatedDevice,
                onChangeConfiguration = { simulatedDevice = it },
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
                Text(deviceLabel(simulatedDevice.device), modifier = Modifier.padding(start = 10.dp), fontWeight = FontWeight.Medium)
            }

        }
    }
}

/** Where the preview's screenshots are recorded, on a platform that can save them. */
@Composable
private fun rememberDeviceScreenshots(): DeviceScreenshots? = remember { pngDownloader?.let { DeviceScreenshots() } }

/** The device's name, and its size when that is what sets it apart: "Freeform · 834 × 1112". */
private fun deviceLabel(device: Device): String =
    if (device is FreeformDevice) "${device.name} · ${device.size.width.roundToInt()} × ${device.size.height.roundToInt()}" else device.name

/**
 * Shows [content] in a simulated device, with the configuration panel beside it when [size] is
 * expanded, or behind a button below it otherwise.
 *
 * @param panelItems Your own items for the configuration panel, below its settings: see
 * [ConfigurationView].
 */
@Composable
public fun DeviceWithConfigurationView(
    modifier: Modifier = Modifier,
    size: WindowSizeClass,
    panelItems: List<@Composable () -> Unit> = emptyList(),
    content: @Composable () -> Unit
) {
    when (size.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> DeviceWithConfigurationViewExpanded(
            modifier = modifier,
            panelItems = panelItems,
            content = content
        )

        else -> DeviceWithConfigurationViewCompact(
            modifier = modifier,
            panelItems = panelItems,
            content = content
        )
    }
}

/**
 * Shows [content] in a simulated device, with the configuration panel beside it on the web and
 * desktop, or behind a button below it on Android and iOS.
 *
 * @param panelItems Your own items for the configuration panel, below its settings: see
 * [ConfigurationView].
 */
@Composable
public fun DeviceWithConfigurationView(
    modifier: Modifier = Modifier,
    panelItems: List<@Composable () -> Unit> = emptyList(),
    content: @Composable () -> Unit
) {
    when (getSourceSetName()) {
        "androidMain", "iosMain" -> DeviceWithConfigurationViewCompact(
            modifier = modifier,
            panelItems = panelItems,
            content = content
        )

        else -> DeviceWithConfigurationViewExpanded(
            modifier = modifier,
            panelItems = panelItems,
            content = content
        )
    }
}
