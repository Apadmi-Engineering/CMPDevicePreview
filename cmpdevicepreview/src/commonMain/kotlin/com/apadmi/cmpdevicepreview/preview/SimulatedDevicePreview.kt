package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.apadmi.cmpdevicepreview.core.Appearance
import com.apadmi.cmpdevicepreview.core.Backdrop
import com.apadmi.cmpdevicepreview.core.DevicePreset
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.core.isTouchscreen
import com.apadmi.cmpdevicepreview.devices.DevicePresets

/** How much of the space, in each direction, the device may fill: the rest is margin. */
private const val DeviceFraction = 0.9f

/** The device the preview starts with. */
internal val DefaultDevice: DevicePreset = DevicePresets.galaxyS25

/** The backdrop behind the simulated device. */
internal object DotGridDefaults {
    val Background = Color(0xFFF2F2F4)
    val Dot = Color(0xFFC8C8D0)
    val DarkBackground = Color(0xFF18181C)
    val DarkDot = Color(0xFF2E2E36)
    val Spacing = 20.dp
    val DotSize = 2.dp
}

/**
 * Fills the node with [background] and a grid of round dots, [spacing] apart and centred on it.
 *
 * @param background The colour that fills the node.
 * @param dot The colour of the dots.
 * @param spacing The distance between neighbouring dots.
 * @param dotSize The diameter of a dot.
 * @return This modifier, followed by one that draws the background and the dots behind the content.
 */
internal fun Modifier.dotGrid(
    background: Color = DotGridDefaults.Background,
    dot: Color = DotGridDefaults.Dot,
    spacing: Dp = DotGridDefaults.Spacing,
    dotSize: Dp = DotGridDefaults.DotSize,
): Modifier = drawWithCache {
    val points = dotGridPoints(size, spacing.toPx())
    val strokeWidth = dotSize.toPx()
    onDrawBehind {
        drawRect(background)
        drawPoints(points, PointMode.Points, dot, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

private fun PreviewSettings.isBackdropDark(isDeviceDark: Boolean): Boolean = when (backdrop) {
    Backdrop.Light -> false
    Backdrop.Dark -> true
    Backdrop.Auto -> isDeviceDark
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

/**
 * The dots of a grid [spacing] apart covering [size], with one on its centre.
 *
 * @param size The area the grid covers, in pixels.
 * @param spacing The distance between neighbouring dots, in pixels.
 * @return The position of each dot, in pixels from the top left of the area, row by row. Empty when
 * [size] is empty or [spacing] isn't positive.
 */
internal fun dotGridPoints(size: Size, spacing: Float): List<Offset> {
    if (size.isEmpty() || spacing <= 0f) {
        return emptyList()
    }
    val firstX = (size.width / 2f) % spacing
    val firstY = (size.height / 2f) % spacing
    val columns = ((size.width - firstX) / spacing).toInt() + 1
    val rows = ((size.height - firstY) / spacing).toInt() + 1
    return buildList(columns * rows) {
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                add(Offset(firstX + column * spacing, firstY + row * spacing))
            }
        }
    }
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
    val isDeviceDark = simulatedDevice.configuration.appearance == Appearance.Dark
    val viewport = remember { PreviewViewport() }
    // Like the zoom, turning drag to scroll on or off holds whichever device is shown.
    var mouseAsTouch by remember { mutableStateOf<Boolean?>(null) }
    val isMouseAsTouch = mouseAsTouch ?: simulatedDevice.device.isTouchscreen
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
                isMouseAsTouch = isMouseAsTouch,
                onResize = onResize?.let { resize ->
                    { freeform -> resize(simulatedDevice.copy(device = freeform).adaptedToDevice()) }
                },
                onLayoutScale = { viewport.fitScale = it },
                content = content,
            )
        }
        if (isZoomable) {
            // How the device is viewed goes in one corner, and what can be done with it in the other.
            PreviewToolbar(
                viewport,
                previewSettings.isBackdropDark(isDeviceDark),
                Modifier.align(Alignment.BottomStart).padding(8.dp),
                isDragToScroll = isMouseAsTouch.takeIf { mouseAsTouchRelabeller != null },
                onDragToScrollChange = { mouseAsTouch = it },
            )
            ScreenshotMenu(
                simulatedDevice.device,
                previewSettings.isBackdropDark(isDeviceDark),
                Modifier.align(Alignment.BottomEnd).padding(8.dp),
            )
        }
    }
}
