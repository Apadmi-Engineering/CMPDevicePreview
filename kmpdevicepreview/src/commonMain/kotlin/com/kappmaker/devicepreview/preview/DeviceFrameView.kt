package com.kappmaker.devicepreview.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kappmaker.devicepreview.core.Device
import com.kappmaker.devicepreview.frame.DeviceFramePainter
import com.kappmaker.devicepreview.frame.DeviceScreenShape
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The safe area of the device showing the content — the notch, Dynamic Island, status bar and
 * home indicator areas it should keep clear of — in the current orientation.
 *
 * Inside a [DeviceFrameView] (and so inside [SimulatedDevicePreview]) this is the simulated
 * device's; [DeviceWithConfigurationView] provides the real `WindowInsets.safeDrawing` when its
 * preview mode is off. Use it like any insets:
 *
 * ```kotlin
 * Modifier.windowInsetsPadding(SimulatedDeviceSafeArea.current)
 * ```
 */
public val SimulatedDeviceSafeArea: ProvidableCompositionLocal<WindowInsets> =
    compositionLocalOf { WindowInsets(0, 0, 0, 0) }

/**
 * Draws [device] around [content]: the body artwork behind the screen and the screen outline
 * (rounded corners, notch, punch-hole or Dynamic Island) as a clip on the content.
 *
 * The content is laid out at the device's logical screen size — its dp are the device's — and the
 * whole device is scaled to fit the available space. [SimulatedDeviceSafeArea] gives the content
 * the device's safe area for the current orientation.
 *
 * @param isPortrait Landscape rotates the device a quarter turn, frame included.
 * @param isFrameVisible When false the body is not drawn; the content is still clipped to the
 * device's screen outline.
 */
@Composable
public fun DeviceFrameView(
    device: Device,
    modifier: Modifier = Modifier,
    isPortrait: Boolean = true,
    isFrameVisible: Boolean = true,
    content: @Composable () -> Unit,
) {
    val frame = device.frame
    val painter = remember(frame) { frame?.let(::DeviceFramePainter) }
    val portraitScreen = device.screenSize.let { Size(it.width / it.density, it.height / it.density) }
    val screenSize = if (isPortrait) portraitScreen else Size(portraitScreen.height, portraitScreen.width)
    val safeArea = if (isPortrait) device.portraitSafeArea else device.landscapeSafeArea

    val bodyPainter = painter?.takeIf { isFrameVisible && it.body != null }
    // What has to stay visible, in screen coordinates: the whole body when it is drawn.
    val bounds = bodyPainter?.frame?.bodyBounds(screenSize, isPortrait) ?: Rect(Offset.Zero, screenSize)

    val screenShape = remember(painter, screenSize, isPortrait) {
        painter?.screenClip(screenSize, isPortrait)?.let { DeviceScreenShape(it, screenSize) }
    }
    val screenModifier = when {
        screenShape != null -> Modifier.clip(screenShape)
        // Devices without any artwork keep a simple outline.
        frame == null -> RoundedCornerShape(8.dp).let { shape ->
            Modifier.border(shape = shape, border = BorderStroke(2.dp, Color.Black)).clip(shape)
        }
        else -> Modifier.clipToBounds()
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // Pixels per logical pixel of the simulated device.
        val scale = fitScale(constraints, bounds.size, fallback = LocalDensity.current.density)
        val simulatedDensity = remember(scale) { Density(density = scale, fontScale = 1f) }
        val bodyModifier = if (bodyPainter != null) {
            Modifier.drawBehind {
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.scale(scale, scale)
                    canvas.translate(-bounds.left, -bounds.top)
                    bodyPainter.paintBody(canvas, screenSize, isPortrait)
                    canvas.restore()
                }
            }
        } else {
            Modifier
        }

        Layout(
            modifier = bodyModifier,
            content = {
                Box(screenModifier) {
                    CompositionLocalProvider(
                        LocalDensity provides simulatedDensity,
                        SimulatedDeviceSafeArea provides safeArea.toWindowInsets(),
                    ) {
                        content()
                    }
                }
            },
        ) { measurables, _ ->
            val screen = measurables.single().measure(
                Constraints.fixed(
                    width = (screenSize.width * scale).roundToInt(),
                    height = (screenSize.height * scale).roundToInt(),
                )
            )
            layout((bounds.width * scale).roundToInt(), (bounds.height * scale).roundToInt()) {
                screen.place((-bounds.left * scale).roundToInt(), (-bounds.top * scale).roundToInt())
            }
        }
    }
}

/** The largest scale at which [content] fits [constraints], or [fallback] when unbounded. */
private fun fitScale(constraints: Constraints, content: Size, fallback: Float): Float {
    val horizontal = if (constraints.hasBoundedWidth) constraints.maxWidth / content.width else Float.POSITIVE_INFINITY
    val vertical = if (constraints.hasBoundedHeight) constraints.maxHeight / content.height else Float.POSITIVE_INFINITY
    val scale = min(horizontal, vertical)
    return if (scale.isFinite() && scale > 0f) scale else fallback
}
