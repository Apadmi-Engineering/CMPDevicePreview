package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.core.adaptedToDevice

/** The narrowest the view can be and still show the panel beside the device. */
private val SidePanelMinWidth = 840.dp

/**
 * Shows [content] in a simulated device, with the configuration panel beside it when there is
 * room, or behind a button below it otherwise. The panel's settings, and [content]'s own state,
 * are kept when the view is resized.
 *
 * Turning preview mode off in the panel shows just [content] until it is composed again, such as
 * by reloading the page.
 *
 * @param modifier The modifier for the view as a whole, which holds the device and the panel.
 * @param initialDevice The device shown first: a built-in preset, or a [Device] of your own. The
 * panel's picker offers the built-in presets.
 * @param initialConfiguration How the device is shown at first.
 * @param initialPreviewSettings How the preview around the device is shown at first.
 * @param panelItems Your own items for the configuration panel, below its settings: see
 * [ConfigurationView].
 * @param content What is shown inside the simulated device, or on its own when preview mode is off.
 */
@Composable
public fun DeviceWithConfigurationView(
    modifier: Modifier = Modifier,
    initialDevice: Device = DefaultDevice,
    initialConfiguration: DeviceConfiguration = DeviceConfiguration(),
    initialPreviewSettings: PreviewSettings = PreviewSettings(),
    panelItems: List<@Composable () -> Unit> = emptyList(),
    content: @Composable () -> Unit,
) {
    val simulatedDeviceSaver = remember(initialDevice) { simulatedDeviceSaver(initialDevice) }
    var simulatedDevice by rememberSaveable(stateSaver = simulatedDeviceSaver) {
        mutableStateOf(SimulatedDevice(initialDevice, initialConfiguration).adaptedToDevice())
    }
    var previewSettings by rememberSaveable(stateSaver = PreviewSettingsSaver) {
        mutableStateOf(initialPreviewSettings)
    }
    if (!previewSettings.isPreviewModeEnabled) {
        content()
        return
    }
    val onSimulatedDeviceChange: (SimulatedDevice) -> Unit = { simulatedDevice = it }
    val onPreviewSettingsChange: (PreviewSettings) -> Unit = { previewSettings = it }
    CompositionLocalProvider(LocalDeviceScreenshots provides rememberDeviceScreenshots()) {
        BoxWithConstraints(modifier) {
            val isWide = maxWidth >= SidePanelMinWidth
            // The device keeps its place in the composition whichever way the panel is shown, so
            // neither it nor the content is recreated when the view crosses the width above.
            PreviewLayout(isWide) {
                SimulatedDevicePreview(
                    simulatedDevice = simulatedDevice,
                    previewSettings = previewSettings,
                    onResize = onSimulatedDeviceChange,
                    content = content,
                )
                // PreviewLayout measures exactly two children, so the panel stays one node even
                // while it shows a dialog.
                Box {
                    if (isWide) {
                        ConfigurationView(
                            simulatedDevice = simulatedDevice,
                            onSimulatedDeviceChange = onSimulatedDeviceChange,
                            previewSettings = previewSettings,
                            onPreviewSettingsChange = onPreviewSettingsChange,
                            modifier = Modifier.fillMaxSize(),
                            panelItems = panelItems,
                        )
                    } else {
                        PanelSheet(simulatedDevice, onSimulatedDeviceChange, previewSettings, onPreviewSettingsChange, panelItems)
                    }
                }
            }
        }
    }
}

/**
 * Lays out the device first and the panel second: the panel [PanelWidth] wide on the trailing
 * side when [isWide], otherwise below the device at its own height.
 */
@Composable
private fun PreviewLayout(isWide: Boolean, content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val (preview, panel) = measurables
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight
        if (isWide) {
            val panelWidth = PanelWidth.roundToPx().coerceAtMost(width)
            val previewWidth = width - panelWidth
            val previewPlaceable = preview.measure(Constraints.fixed(previewWidth, height))
            val panelPlaceable = panel.measure(Constraints.fixed(panelWidth, height))
            layout(width, height) {
                previewPlaceable.placeRelative(0, 0)
                panelPlaceable.placeRelative(previewWidth, 0)
            }
        } else {
            val panelPlaceable = panel.measure(Constraints(minWidth = width, maxWidth = width, maxHeight = height))
            val previewHeight = (height - panelPlaceable.height).coerceAtLeast(0)
            val previewPlaceable = preview.measure(Constraints.fixed(width, previewHeight))
            layout(width, height) {
                previewPlaceable.placeRelative(0, 0)
                panelPlaceable.placeRelative(0, previewHeight)
            }
        }
    }
}

/** A button naming the device, which opens the panel in a bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanelSheet(
    simulatedDevice: SimulatedDevice,
    onSimulatedDeviceChange: (SimulatedDevice) -> Unit,
    previewSettings: PreviewSettings,
    onPreviewSettingsChange: (PreviewSettings) -> Unit,
    panelItems: List<@Composable () -> Unit>,
) {
    var isSheetShown by remember { mutableStateOf(false) }
    if (isSheetShown) {
        ModalBottomSheet(
            onDismissRequest = { isSheetShown = false },
            dragHandle = { BottomSheetDefaults.DragHandle(color = PanelColors.SwitchOff) },
            containerColor = PanelColors.Background,
        ) {
            ConfigurationView(
                simulatedDevice = simulatedDevice,
                onSimulatedDeviceChange = onSimulatedDeviceChange,
                previewSettings = previewSettings,
                onPreviewSettingsChange = onPreviewSettingsChange,
                modifier = Modifier.fillMaxSize(),
                panelItems = panelItems,
            )
        }
    }
    Button(
        onClick = { isSheetShown = true },
        modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PanelColors.Background,
            contentColor = PanelColors.TextPrimary,
        ),
    ) {
        Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(deviceLabel(simulatedDevice.device), modifier = Modifier.padding(start = 10.dp), fontWeight = FontWeight.Medium)
    }
}

/** Where the preview's screenshots are recorded, on a platform that can save them. */
@Composable
private fun rememberDeviceScreenshots(): DeviceScreenshots? = remember { pngDownloader?.let { DeviceScreenshots() } }

/** The device's name, with its size for a freeform window: "Freeform · 834 × 1112". */
private fun deviceLabel(device: Device): String =
    if (device is FreeformDevice) "${device.name} · ${device.size.dimensionsLabel()}" else device.name
