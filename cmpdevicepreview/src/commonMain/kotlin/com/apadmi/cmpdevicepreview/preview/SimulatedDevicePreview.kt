package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.apadmi.cmpdevicepreview.core.Backdrop
import com.apadmi.cmpdevicepreview.core.DevicePreset
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.devices.DevicePresets

/** How much of the space, in each direction, the device may fill: the rest is margin. */
private const val DeviceFraction = 0.9f

/** The device the preview starts with. */
internal val DefaultDevice: DevicePreset = DevicePresets.galaxyS25

private fun PreviewSettings.isBackdropDark(isDeviceDark: Boolean): Boolean = when (backdrop) {
    Backdrop.LIGHT -> false
    Backdrop.DARK -> true
    Backdrop.AUTO -> isDeviceDark
}

/** The backdrop [settings] asks for: light or dark, with or without dots. */
private fun Modifier.backdrop(settings: PreviewSettings, isDeviceDark: Boolean): Modifier {
    val isDark = settings.isBackdropDark(isDeviceDark)
    val background = if (isDark) DotGridDefaults.DarkBackground else DotGridDefaults.Background
    return if (settings.isDotGridVisible) {
        dotGrid(background = background, dot = if (isDark) DotGridDefaults.DarkDot else DotGridDefaults.Dot)
    } else {
        background(background)
    }
}

/**
 * Shows [simulatedDevice] with [content] inside it, on the backdrop [previewSettings] asks for.
 *
 * @param modifier The modifier for the preview, which the backdrop fills.
 * @param simulatedDevice The device to show and how it is shown.
 * @param previewSettings How the backdrop behind the device is shown.
 * @param onResize When set, the device can be resized by dragging its corners, and this gets the
 * resized device to pass back as [simulatedDevice].
 * @param content What is shown inside the device.
 */
@Composable
public fun SimulatedDevicePreview(
    modifier: Modifier = Modifier,
    simulatedDevice: SimulatedDevice = SimulatedDevice(DefaultDevice),
    previewSettings: PreviewSettings = PreviewSettings(),
    onResize: ((SimulatedDevice) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ZoomableDevicePreview(simulatedDevice, previewSettings, onResize, isZoomable = false, modifier, content)
}

@Composable
internal fun ZoomableDevicePreview(
    simulatedDevice: SimulatedDevice,
    previewSettings: PreviewSettings,
    onResize: ((SimulatedDevice) -> Unit)?,
    isZoomable: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isDeviceDark = simulatedDevice.configuration.isDarkMode
    val viewport = remember { PreviewViewport() }
    val zoomGestures = if (isZoomable) {
        Modifier.clipToBounds().onSizeChanged { viewport.resize(it.toSize()) }.zoomAndPan(viewport)
    } else {
        Modifier
    }
    val zoomTransform = if (isZoomable) {
        Modifier.graphicsLayer {
            transformOrigin = TransformOrigin(0f, 0f)
            scaleX = viewport.zoom
            scaleY = viewport.zoom
            translationX = viewport.pan.x
            translationY = viewport.pan.y
        }
    } else {
        Modifier
    }
    Box(
        modifier = modifier.fillMaxSize().backdrop(previewSettings, isDeviceDark).then(zoomGestures),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().then(zoomTransform), contentAlignment = Alignment.Center) {
            DeviceFrame(
                device = simulatedDevice.device,
                modifier = Modifier.padding(16.dp).fillMaxSize(DeviceFraction),
                configuration = simulatedDevice.configuration,
                onResize = onResize?.let { resize ->
                    { freeform -> resize(simulatedDevice.copy(device = freeform).adaptedToDevice()) }
                },
                onLayoutScale = { viewport.fitScale = it },
                content = content,
            )
        }
        if (isZoomable) {
            PreviewToolbar(
                viewport,
                simulatedDevice.device,
                previewSettings.isBackdropDark(isDeviceDark),
                Modifier.align(Alignment.BottomStart).padding(8.dp),
            )
        }
    }
}
