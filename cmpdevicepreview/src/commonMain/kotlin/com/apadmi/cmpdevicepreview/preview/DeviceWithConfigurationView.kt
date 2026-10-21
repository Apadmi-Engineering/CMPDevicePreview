@file:Suppress("FILE_NAME_MATCH_CLASS")

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

/**
 * What the view hands its configuration panel, beside the device or in the sheet.
 *
 * @property spec The panel.
 * @property state What the panel shows, fitted to it.
 * @property initial What the panel's reset button returns to.
 * @property languages The languages the host offers.
 * @property onSimulatedDeviceChange Applies a changed device or configuration.
 * @property onPreviewSettingsChange Applies changed preview settings.
 * @property onSettingValuesChange Applies changed values of settings of your own.
 */
private class PanelHostState(
    val spec: PanelSpec,
    val state: PreviewState,
    val initial: PreviewState,
    val languages: List<PreviewLanguage>,
    val onSimulatedDeviceChange: (SimulatedDevice) -> Unit,
    val onPreviewSettingsChange: (PreviewSettings) -> Unit,
    val onSettingValuesChange: (PanelSettingValues) -> Unit,
)

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
 * @param panel What the configuration panel shows, in order: see [PanelScope]. By default it is the
 * usual panel. A control it leaves out is hidden, and its setting stays as [initialDevice],
 * [initialConfiguration] and [initialPreviewSettings] set it, whatever a link or the kept settings
 * say. Inside the device, [PanelSetting.current] reads the value of a setting of your own placed in it.
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
    panel: PanelScope.() -> Unit = DefaultPanel,
    isPersistenceEnabled: Boolean = true,
    languages: List<PreviewLanguage> = emptyList(),
    content: @Composable () -> Unit,
) {
    val spec by rememberPanelSpec(panel)
    val store = if (isPersistenceEnabled) LocalSettingsStore.current ?: rememberSettingsStore() else null
    val initial = remember(initialDevice, initialConfiguration, initialPreviewSettings, languages, spec) {
        val given = PreviewState(SimulatedDevice(initialDevice, initialConfiguration), initialPreviewSettings)
        given.fittedTo(spec, languages, given)
    }
    val starting = remember(store) { store.startingState(initial) }
    val simulatedDeviceSaver = remember(initialDevice) { simulatedDeviceSaver(initialDevice) }
    var simulatedDevice by rememberSaveable(stateSaver = simulatedDeviceSaver) { mutableStateOf(starting.simulatedDevice) }
    var previewSettings by rememberSaveable(stateSaver = PreviewSettingsSaver) { mutableStateOf(starting.previewSettings) }
    var settingValues by rememberSaveable(stateSaver = PanelSettingValuesSaver) { mutableStateOf(starting.settingValues) }
    // Wherever the state came from, a link, the kept settings or a change of panel, it is shown as the panel allows.
    val state = remember(simulatedDevice, previewSettings, settingValues, spec, languages, initial) {
        PreviewState(simulatedDevice, previewSettings, settingValues).fittedTo(spec, languages, initial)
    }
    // Before the return below, so the settings are kept while only the content is shown too.
    val encoded = remember(state, initial) { encodeSettings(state, initial) }
    LaunchedEffect(store, encoded) {
        if (store != null) {
            delay(SaveDelayMillis)
            store.write(encoded)
        }
    }
    if (!state.previewSettings.isPreviewModeEnabled) {
        Box(modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalPanelSettingValues provides state.settingValues, content = content)
            // With the switch hidden, preview mode stays as the host started it.
            if (PanelControl.PreviewMode in spec.controls) {
                PreviewModeButton(
                    onClick = { previewSettings = state.previewSettings.copy(isPreviewModeEnabled = true) },
                    modifier = Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
                )
            }
        }
        return
    }
    val panelState = PanelHostState(
        spec = spec,
        state = state,
        initial = initial,
        languages = languages,
        onSimulatedDeviceChange = { simulatedDevice = it },
        onPreviewSettingsChange = { previewSettings = it },
        onSettingValuesChange = { settingValues = it },
    )
    CompositionLocalProvider(
        LocalDeviceScreenshots provides rememberDeviceScreenshots(),
        LocalPanelSettingValues provides state.settingValues,
    ) {
        BoxWithConstraints(modifier) {
            val isWide = maxWidth >= SidePanelMinWidth
            // The device keeps its place in the composition whichever way the panel is shown, so
            // neither it nor the content is recreated when the view crosses the width above.
            PreviewLayout(isWide) {
                ZoomableDevicePreview(
                    simulatedDevice = state.simulatedDevice,
                    previewSettings = state.previewSettings,
                    onResize = panelState.onSimulatedDeviceChange,
                    isZoomable = true,
                    content = content,
                )
                // PreviewLayout measures exactly two children, so the panel stays one node even
                // while it shows a dialog.
                Box {
                    if (isWide) {
                        HostedPanel(panelState, Modifier.fillMaxSize())
                    } else {
                        PanelSheet(panelState)
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
private fun PanelSheet(panel: PanelHostState) {
    var isSheetShown by remember { mutableStateOf(false) }
    if (isSheetShown) {
        ModalBottomSheet(
            onDismissRequest = { isSheetShown = false },
            dragHandle = { BottomSheetDefaults.DragHandle(color = PanelColors.SwitchOff) },
            containerColor = PanelColors.Background,
        ) {
            HostedPanel(panel, Modifier.fillMaxSize())
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
        Text(deviceLabel(panel.state.simulatedDevice.device), modifier = Modifier.padding(start = 10.dp), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HostedPanel(panel: PanelHostState, modifier: Modifier) {
    ConfigurationPanel(
        spec = panel.spec,
        state = panel.state,
        initial = panel.initial,
        languages = panel.languages,
        onSimulatedDeviceChange = panel.onSimulatedDeviceChange,
        onPreviewSettingsChange = panel.onPreviewSettingsChange,
        onSettingValuesChange = panel.onSettingValuesChange,
        modifier = modifier,
    )
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
