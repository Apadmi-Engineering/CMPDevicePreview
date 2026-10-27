package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize

/** Where the [DeviceFrameView] below records its screenshots, or null when it doesn't. */
internal val LocalDeviceScreenshots = staticCompositionLocalOf<DeviceScreenshots?> { null }

/**
 * Saves an image as a PNG: the browser downloads it, and the desktop asks where to save it. Null
 * where the platform has no way to, and there the panel has no screenshot buttons.
 */
internal expect val pngDownloader: PngDownloader?

// Null where the platform can't put an image on the clipboard, and there the screenshot menu only saves.
internal expect val pngCopier: PngCopier?

/** Saves `image` as a PNG called `fileName`. */
internal typealias PngDownloader = (image: ImageBitmap, fileName: String) -> Unit

// Copies image to the clipboard as a PNG, then calls onDone with whether it was copied.
internal typealias PngCopier = (image: ImageBitmap, onDone: (isCopied: Boolean) -> Unit) -> Unit

/**
 * Screenshots of the device a [DeviceFrameView] below [LocalDeviceScreenshots] shows: of its screen,
 * or of the whole device in its frame. The view records both while it is shown and not turning.
 */
internal class DeviceScreenshots {
    private var screen by mutableStateOf<Recording?>(null)
    private var device by mutableStateOf<Recording?>(null)

    /** Whether there is a device to take a screenshot of. */
    val canCapture: Boolean
        get() = screen != null && device != null

    fun attach(screen: Recording, device: Recording) {
        this.screen = screen
        this.device = device
    }

    fun detach() {
        screen = null
        device = null
    }

    /**
     * The screen as the device shows it, status bar and keyboard included, at the device's own
     * resolution; or with [includeFrame], the whole device on a transparent background. Null when
     * no device is shown.
     *
     * @param includeFrame Whether to capture the whole device in its frame, rather than only the
     * screen.
     * @return The screenshot, or null when there is no device to capture or it has no size yet.
     */
    fun capture(includeFrame: Boolean): ImageBitmap? {
        val recording = (if (includeFrame) device else screen) ?: return null
        val drawn = recording.layer.size
        val size = recording.size
        if (drawn.width <= 0 || drawn.height <= 0 || size.width <= 0 || size.height <= 0) {
            return null
        }
        val image = ImageBitmap(size.width, size.height)
        // Drawn again at the new scale rather than resampled, so text and frames stay sharp.
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), size.toSize()) {
            scale(size.width / drawn.width.toFloat(), size.height / drawn.height.toFloat(), pivot = Offset.Zero) {
                drawLayer(recording.layer)
            }
        }
        return image
    }
    /**
     * What the view last drew into [layer], and the [size] a screenshot of it is, in pixels.
     *
     * @property layer The graphics layer the view draws into.
     * @property size The size of a screenshot of the layer, in pixels.
     */
    data class Recording(val layer: GraphicsLayer, val size: IntSize)
}

/**
 * Draws the node's content through [layer], so it can be drawn again for a screenshot.
 *
 * @param layer The graphics layer to record the node's content into.
 * @return This modifier, followed by one that records the content into [layer] as it is drawn.
 */
internal fun Modifier.recordInto(layer: GraphicsLayer): Modifier = drawWithContent {
    layer.record { this@drawWithContent.drawContent() }
    drawLayer(layer)
}
