package com.kappmaker.devicepreview.preview

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kappmaker.devicepreview.core.Device
import com.kappmaker.devicepreview.core.isTouchscreen
import com.kappmaker.devicepreview.core.keyboardHeightIn
import com.kappmaker.devicepreview.core.portraitScreenSize
import com.kappmaker.devicepreview.core.safeAreaIn
import com.kappmaker.devicepreview.frame.DeviceFramePainter
import com.kappmaker.devicepreview.frame.DeviceScreenShape
import com.kappmaker.devicepreview.frame.SystemUiColors
import com.kappmaker.devicepreview.frame.SystemUiPainter
import com.kappmaker.devicepreview.frame.paintSimulatedKeyboard
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws [device] around [content]: the body artwork behind the screen and the screen outline
 * (rounded corners, notch, punch-hole or Dynamic Island) as a clip on the content.
 *
 * The content is laid out at the device's logical screen size — its dp are the device's — and the
 * whole device is scaled to fit the available space.
 *
 * Inside the device, Compose's own APIs describe it, so the content needs nothing from this library:
 * `WindowInsets` (`safeDrawing`, `statusBars`, `navigationBars`, `displayCutout`, `ime`, and the
 * padding modifiers and Material defaults built on them) report its safe area and keyboard, and
 * `isSystemInDarkTheme()` reports [isDarkMode]. Android has no hooks for this: there the content
 * sees the real window's insets and theme. On every platform, `LocalWindowInfo` reports the
 * device's screen as the window, so window size classes (`currentWindowAdaptiveInfo()`) follow it.
 *
 * The device's status bar and gesture pill, and the band of a raised keyboard, are painted over the
 * content. They don't take touches. The bars are decoration: hiding them changes nothing the content
 * can observe.
 *
 * On the web, the mouse acts as a finger on a touchscreen device: dragging the screen scrolls, as
 * it would on the device. The wheel and hover work as usual.
 *
 * @param isPortrait Landscape rotates the device a quarter turn, frame included. A change is
 * animated: the device turns, then the content is laid out in the new orientation. The device
 * keeps its portrait size in landscape, unless it has to shrink to fit.
 * @param isFrameVisible When false the body is not drawn; the content is still clipped to the
 * device's screen outline.
 * @param isSystemUiVisible Whether the status bar and gesture pill are drawn. The safe area stays
 * the same either way.
 * @param isKeyboardVisible Whether the device's software keyboard is raised, which draws its band
 * and adds it to the insets. Has no effect on a device without a keyboard height for this
 * orientation.
 * @param isDarkMode Whether the simulated system is in dark mode: what `isSystemInDarkTheme()`
 * returns inside the device, and the colour of the status bar's icons.
 */
@Composable
public fun DeviceFrameView(
    device: Device,
    modifier: Modifier = Modifier,
    isPortrait: Boolean = true,
    isFrameVisible: Boolean = true,
    isSystemUiVisible: Boolean = true,
    isKeyboardVisible: Boolean = false,
    isDarkMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    // A change of orientation turns the device a quarter turn, then lays it out in the new
    // orientation: until then the content keeps the old one, as on a phone being turned.
    var layoutPortrait by remember { mutableStateOf(isPortrait) }
    // Degrees the device is turned from its layout orientation, clockwise.
    var turn by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPortrait) {
        val target = when {
            isPortrait == layoutPortrait -> 0f
            // Landscape is portrait turned counter-clockwise.
            isPortrait -> 90f
            else -> -90f
        }
        if (turn != target) {
            animate(turn, target, animationSpec = tween(TurnMillis, easing = FastOutSlowInEasing)) { value, _ ->
                turn = value
            }
        }
        if (isPortrait != layoutPortrait) {
            layoutPortrait = isPortrait
            turn = 0f
        }
    }

    val frame = device.frame
    val painter = remember(frame) { frame?.let(::DeviceFramePainter) }
    val portraitScreen = device.portraitScreenSize
    val screenSize = if (layoutPortrait) portraitScreen else Size(portraitScreen.height, portraitScreen.width)
    val safeArea = device.safeAreaIn(layoutPortrait)
    val keyboardInset = if (isKeyboardVisible) device.keyboardHeightIn(layoutPortrait) ?: 0f else 0f

    val bodyPainter = painter?.takeIf { isFrameVisible && it.body != null }
    // What has to stay visible, in screen coordinates: the whole body when it is drawn.
    val bounds = bodyPainter?.frame?.bodyBounds(screenSize, layoutPortrait) ?: Rect(Offset.Zero, screenSize)

    val screenShape = remember(painter, screenSize, layoutPortrait) {
        painter?.screenClip(screenSize, layoutPortrait)?.let { DeviceScreenShape(it, screenSize) }
    }
    val screenModifier = when {
        screenShape != null -> Modifier.clip(screenShape)
        // Devices without any artwork keep a simple outline.
        frame == null -> RoundedCornerShape(8.dp).let { shape ->
            Modifier.border(shape = shape, border = BorderStroke(2.dp, Color.Black)).clip(shape)
        }
        else -> Modifier.clipToBounds()
    }

    val systemUiPainter = remember(device.systemUi) {
        device.systemUi?.takeUnless { it.isEmpty }?.let(::SystemUiPainter)
    }.takeIf { isSystemUiVisible }
    // Drawn over the content and inside the screen clip, so the screen outline cuts it too. The
    // bars use the device's own safe area: the gesture pill stays on top of the keyboard band.
    val overlayModifier = if (systemUiPainter != null || keyboardInset > 0f) {
        val iconColor = SystemUiColors.icons(isDarkMode)
        Modifier.drawWithContent {
            drawContent()
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.scale(size.width / screenSize.width, size.height / screenSize.height)
                paintSimulatedKeyboard(canvas, screenSize, keyboardInset)
                systemUiPainter?.paint(canvas, screenSize, safeArea, iconColor, layoutDirection)
                canvas.restore()
            }
        }
    } else {
        Modifier
    }
    val inputModifier = if (device.isTouchscreen) Modifier.mouseAsTouch() else Modifier

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val fallback = LocalDensity.current.density
        val portraitBounds = if (layoutPortrait) bounds.size else Size(bounds.height, bounds.width)
        val layoutDegrees = if (layoutPortrait) 0f else -90f
        // Pixels per logical pixel of the simulated device.
        val scale = turnedFitScale(constraints, portraitBounds, layoutDegrees, fallback)
        // Mid-turn the device shrinks a little to stay inside the space. Only the layer is scaled,
        // so the content isn't laid out again on every frame of the turn.
        val turnModifier = Modifier.graphicsLayer {
            val degrees = turn
            val turnScale = turnedFitScale(constraints, portraitBounds, layoutDegrees + degrees, fallback) / scale
            rotationZ = degrees
            scaleX = turnScale
            scaleY = turnScale
        }
        val simulatedDensity = remember(scale) { Density(density = scale, fontScale = 1f) }
        // The window the content sees is the device's screen, so window size classes follow it.
        val hostWindowInfo = LocalWindowInfo.current
        val simulatedWindowInfo = remember(hostWindowInfo, screenSize, scale) {
            SimulatedWindowInfo(hostWindowInfo, screenSize, scale)
        }
        val bodyModifier = if (bodyPainter != null) {
            Modifier.drawBehind {
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.scale(scale, scale)
                    canvas.translate(-bounds.left, -bounds.top)
                    bodyPainter.paintBody(canvas, screenSize, layoutPortrait)
                    canvas.restore()
                }
            }
        } else {
            Modifier
        }

        Layout(
            modifier = turnModifier.then(bodyModifier),
            content = {
                Box(screenModifier.then(overlayModifier).then(inputModifier)) {
                    CompositionLocalProvider(
                        LocalDensity provides simulatedDensity,
                        LocalWindowInfo provides simulatedWindowInfo,
                    ) {
                        SimulatedPlatform(safeArea, keyboardInset, isDarkMode, content)
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

/** The host's window, resized to a device screen of [screenSize] logical pixels at [scale]. */
private class SimulatedWindowInfo(
    private val host: WindowInfo,
    screenSize: Size,
    scale: Float,
) : WindowInfo {
    override val isWindowFocused: Boolean get() = host.isWindowFocused
    override val containerSize: IntSize =
        IntSize((screenSize.width * scale).roundToInt(), (screenSize.height * scale).roundToInt())
    override val containerDpSize: DpSize = DpSize(screenSize.width.dp, screenSize.height.dp)
}

/** How long the device takes to turn between portrait and landscape. */
private const val TurnMillis = 450

/**
 * The scale at which a device whose portrait bounds are [portraitBounds] fits [constraints] when
 * turned [degrees] clockwise from portrait, and never larger than in portrait: turning a device
 * doesn't make it bigger, though it shrinks when it would otherwise not fit.
 */
internal fun turnedFitScale(constraints: Constraints, portraitBounds: Size, degrees: Float, fallback: Float): Float {
    val radians = degrees * PI.toFloat() / 180f
    val cos = abs(cos(radians))
    val sin = abs(sin(radians))
    val turned = Size(
        width = portraitBounds.width * cos + portraitBounds.height * sin,
        height = portraitBounds.width * sin + portraitBounds.height * cos,
    )
    return min(fitScale(constraints, portraitBounds, fallback), fitScale(constraints, turned, fallback))
}

/** The largest scale at which [content] fits [constraints], or [fallback] when unbounded. */
private fun fitScale(constraints: Constraints, content: Size, fallback: Float): Float {
    val horizontal = if (constraints.hasBoundedWidth) constraints.maxWidth / content.width else Float.POSITIVE_INFINITY
    val vertical = if (constraints.hasBoundedHeight) constraints.maxHeight / content.height else Float.POSITIVE_INFINITY
    val scale = min(horizontal, vertical)
    return if (scale.isFinite() && scale > 0f) scale else fallback
}
