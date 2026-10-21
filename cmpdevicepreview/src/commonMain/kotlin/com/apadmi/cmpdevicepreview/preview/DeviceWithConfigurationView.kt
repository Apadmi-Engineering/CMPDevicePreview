package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewLanguage
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import kotlinx.coroutines.delay

private const val ButtonGlowAlpha = 0.45f

// Long enough that dragging a slider or a corner is kept once, which also keeps the URL under Safari's limit on changing it.
private const val SaveDelayMillis = 250L

/** The narrowest the view can be and still show the panel beside the device. */
private val SidePanelMinWidth = 880.dp

/** How far the dark glow around the button that turns preview mode back on reaches from its centre. */
private val ButtonGlowRadius = 96.dp

// A link's settings are the whole truth, so what it leaves out is the initial setting, not a kept one.
private fun SettingsStore?.startingState(initial: PreviewState): PreviewState {
    val encoded = this?.let { it.readLink()?.takeIf(String::isNotEmpty) ?: it.readSaved() }
    return encoded?.let { decodeSettings(it, initial) } ?: initial
}

/**
 * Shows [content] in a simulated device, with the configuration panel beside it when there is
 * room, or behind a button below it otherwise. The panel's settings, and [content]'s own state,
 * are kept when the view is resized.
 *
 * Turning preview mode off in the panel shows just [content], with a button in the corner to turn
 * it back on. [content] is composed afresh each time the mode changes.
 *
 * @param modifier The modifier for the view as a whole, which holds the device and the panel.
 * @param initialDevice The device shown first: a built-in preset, or a [Device] of your own. The
 * panel's picker offers the built-in presets.
 * @param initialConfiguration How the device is shown at first.
 * @param initialPreviewSettings How the preview around the device is shown at first.
 * @param panelItems Your own items for the configuration panel, below its settings: see
 * [ConfigurationView].
 * @param panelTitle The title in the configuration panel's header: see [ConfigurationView].
 * @param panelIcon The icon beside the title in the configuration panel's header: see [ConfigurationView].
 * @param isAttributionVisible Whether the configuration panel ends with a "Built with CMPDevicePreview"
 * link to the project's GitHub repository: see [ConfigurationView]. Pass false to hide it.
 * @param isPersistenceEnabled Whether what differs from the initial settings is kept across reloads
 * and restarts, and on the web in the page's URL as `dp.` query parameters, so a link opens the same
 * preview. A link's settings win over the kept ones. Pass false to turn both off.
 * @param languages The languages the panel offers for the content inside the device: see
 * [ConfigurationView]. With none, the panel has no language setting, whatever a link or the kept
 * settings say.
 * @param content What is shown inside the simulated device, or on its own when preview mode is off.
 */
@Composable
public fun DeviceWithConfigurationView(
    modifier: Modifier = Modifier,
    initialDevice: Device = DefaultDevice,
    initialConfiguration: DeviceConfiguration = DeviceConfiguration(),
    initialPreviewSettings: PreviewSettings = PreviewSettings(),
    panelItems: List<@Composable () -> Unit> = emptyList(),
    panelTitle: String = DefaultPanelTitle,
    panelIcon: @Composable () -> Unit = DefaultPanelIcon,
    isAttributionVisible: Boolean = true,
    isPersistenceEnabled: Boolean = true,
    languages: List<PreviewLanguage> = emptyList(),
    content: @Composable () -> Unit,
) {
    val store = if (isPersistenceEnabled) LocalSettingsStore.current ?: rememberSettingsStore() else null
    val initial = remember(initialDevice, initialConfiguration, initialPreviewSettings, languages) {
        PreviewState(
            SimulatedDevice(initialDevice, initialConfiguration).adaptedToDevice().inLanguages(languages),
            initialPreviewSettings,
        )
    }
    val starting = remember(store) { store.startingState(initial) }
    val simulatedDeviceSaver = remember(initialDevice) { simulatedDeviceSaver(initialDevice) }
    var simulatedDevice by rememberSaveable(stateSaver = simulatedDeviceSaver) {
        mutableStateOf(starting.simulatedDevice.inLanguages(languages))
    }
    var previewSettings by rememberSaveable(stateSaver = PreviewSettingsSaver) {
        mutableStateOf(starting.previewSettings)
    }
    // Before the return below, so the settings are kept while only the content is shown too.
    val encoded = remember(simulatedDevice, previewSettings, initial) {
        encodeSettings(PreviewState(simulatedDevice, previewSettings), initial)
    }
    LaunchedEffect(store, encoded) {
        if (store != null) {
            delay(SaveDelayMillis)
            store.write(encoded)
        }
    }
    if (!previewSettings.isPreviewModeEnabled) {
        Box(modifier.fillMaxSize()) {
            content()
            PreviewModeButton(
                onClick = { previewSettings = previewSettings.copy(isPreviewModeEnabled = true) },
                modifier = Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
            )
        }
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
                ZoomableDevicePreview(
                    simulatedDevice = simulatedDevice,
                    previewSettings = previewSettings,
                    onResize = onSimulatedDeviceChange,
                    isZoomable = true,
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
                            panelTitle = panelTitle,
                            panelIcon = panelIcon,
                            isAttributionVisible = isAttributionVisible,
                            initialSimulatedDevice = initial.simulatedDevice,
                            initialPreviewSettings = initial.previewSettings,
                            languages = languages,
                        )
                    } else {
                        PanelSheet(
                            simulatedDevice,
                            onSimulatedDeviceChange,
                            previewSettings,
                            onPreviewSettingsChange,
                            panelItems,
                            panelTitle,
                            panelIcon,
                            isAttributionVisible,
                            initial,
                            languages,
                        )
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
    panelTitle: String,
    panelIcon: @Composable () -> Unit,
    isAttributionVisible: Boolean,
    initial: PreviewState,
    languages: List<PreviewLanguage>,
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
                panelTitle = panelTitle,
                panelIcon = panelIcon,
                isAttributionVisible = isAttributionVisible,
                initialSimulatedDevice = initial.simulatedDevice,
                initialPreviewSettings = initial.previewSettings,
                languages = languages,
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

@Composable
private fun PreviewModeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    DevicePreviewLogo(
        modifier = modifier
            .drawBehind {
                val radius = ButtonGlowRadius.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(0f to Color.Black.copy(alpha = ButtonGlowAlpha), 1f to Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand),
        size = 44.dp,
        shape = CircleShape,
        contentDescription = "Turn preview mode on",
    )
}

/** Where the preview's screenshots are recorded, on a platform that can save them. */
@Composable
private fun rememberDeviceScreenshots(): DeviceScreenshots? = remember { pngDownloader?.let { DeviceScreenshots() } }

/** The device's name, with its size for a freeform window: "Freeform · 834 × 1112". */
private fun deviceLabel(device: Device): String =
    if (device is FreeformDevice) "${device.name} · ${device.size.dimensionsLabel()}" else device.name
