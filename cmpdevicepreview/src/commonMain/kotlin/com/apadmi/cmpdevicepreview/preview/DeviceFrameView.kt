package com.apadmi.cmpdevicepreview.preview

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.div
import com.apadmi.cmpdevicepreview.core.isRotatable
import com.apadmi.cmpdevicepreview.core.isTouchscreen
import com.apadmi.cmpdevicepreview.core.keyboardHeightIn
import com.apadmi.cmpdevicepreview.core.laidOutInPortrait
import com.apadmi.cmpdevicepreview.core.portraitScreenSize
import com.apadmi.cmpdevicepreview.core.safeAreaIn
import com.apadmi.cmpdevicepreview.frame.DeviceFramePainter
import com.apadmi.cmpdevicepreview.frame.DeviceScreenShape
import com.apadmi.cmpdevicepreview.frame.SystemUiColors
import com.apadmi.cmpdevicepreview.frame.SystemUiPainter
import com.apadmi.cmpdevicepreview.frame.paintSimulatedKeyboard
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
 * keeps its portrait size in landscape, unless it has to shrink to fit. A desktop window can't be
 * turned: it ignores this and stays in its natural, wide shape.
 * @param isFrameVisible When false the body is not drawn; the content is still clipped to the
 * device's screen outline.
 * @param isSystemUiVisible Whether the status bar and gesture pill are drawn. The safe area stays
 * the same either way.
 * @param isKeyboardVisible Whether the device's software keyboard is raised, which draws its band
 * and adds it to the insets. Has no effect on a device without a keyboard height for this
 * orientation.
 * @param isDarkMode Whether the simulated system is in dark mode: what `isSystemInDarkTheme()`
 * returns inside the device, and the colour of the status bar's icons.
 * @param fontScale How much larger text is than at the default text size: what
 * `LocalDensity.current.fontScale` reports inside the device. Text grows by exactly this, at every
 * size: Compose scales it linearly outside Android, where Android 14 and later grow large text by
 * less.
 * @param displayScale How much larger the content is drawn, as Android's Display size setting
 * does: its window, and its insets, are this much smaller in dp. The frame, status bar and keyboard
 * band don't change.
 * @param layoutDirection The layout direction inside the device. The status bar follows it.
 * @param onResize When set, the device can be resized by dragging its corners (and, for a
 * [FreeformDevice], its edges). While a handle is dragged an outline shows the size the device
 * will take and the content isn't laid out again; on release this is called with the new
 * [FreeformDevice], and the device keeps the scale it had when the drag began instead of fitting
 * the space again, so it stays as big on the screen as it was drawn. Nothing is resized unless the
 * caller passes the new device back. When null, there are no handles. The view leaves room for
 * the handles around the device.
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
    fontScale: Float = 1f,
    displayScale: Float = 1f,
    layoutDirection: LayoutDirection = LocalLayoutDirection.current,
    onResize: ((FreeformDevice) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // A device that can't rotate, like a desktop window, stays in its natural orientation.
    val wantsPortrait = device.laidOutInPortrait(isPortrait)
    // A change of orientation turns the device a quarter turn, then lays it out in the new
    // orientation: until then the content keeps the old one, as on a phone being turned. Swapping
    // a device that can turn for one that can't (or back) starts over rather than turning it.
    val isRotatable = device.isRotatable
    var layoutPortrait by remember(isRotatable) { mutableStateOf(wantsPortrait) }
    // Degrees the device is turned from its layout orientation, clockwise.
    var turn by remember(isRotatable) { mutableFloatStateOf(0f) }
    // Read as a flag, so the view isn't composed again on every frame of a turn.
    val isTurning by remember(isRotatable) { derivedStateOf { turn != 0f } }
    LaunchedEffect(wantsPortrait) {
        val target = when {
            wantsPortrait == layoutPortrait -> 0f
            // Landscape is portrait turned counter-clockwise.
            wantsPortrait -> 90f
            else -> -90f
        }
        if (turn != target) {
            animate(turn, target, animationSpec = tween(TurnMillis, easing = FastOutSlowInEasing)) { value, _ ->
                turn = value
            }
        }
        if (wantsPortrait != layoutPortrait) {
            layoutPortrait = wantsPortrait
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
        // A freeform window is a plain bordered rectangle, with the border switched by the frame.
        device is FreeformDevice -> if (isFrameVisible) {
            RoundedCornerShape(FreeformCornerRadius).let { shape ->
                Modifier
                    .shadow(FreeformShadow, shape, clip = false)
                    .border(shape = shape, border = BorderStroke(1.dp, FreeformBorder))
                    .clip(shape)
            }
        } else {
            Modifier.clipToBounds()
        }
        // Devices without any artwork keep a simple outline.
        frame == null -> RoundedCornerShape(8.dp).let { shape ->
            Modifier.border(shape = shape, border = BorderStroke(2.dp, Color.Black)).clip(shape)
        }
        else -> Modifier.clipToBounds()
    }

    val systemUiPainter = remember(device.systemUi) {
        device.systemUi?.takeUnless { it.isEmpty }?.let(::SystemUiPainter)
    }.takeIf { isSystemUiVisible }
    val deviceLayoutDirection = layoutDirection
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
                // The device's direction, not the host's: a phone in RTL mirrors its status bar.
                systemUiPainter?.paint(canvas, screenSize, safeArea, iconColor, deviceLayoutDirection)
                canvas.restore()
            }
        }
    } else {
        Modifier
    }
    val inputModifier = if (device.isTouchscreen) Modifier.mouseAsTouch() else Modifier

    // The scale a resized device keeps instead of fitting the space again, until the device changes.
    var pinned by remember { mutableStateOf<PinnedScale?>(null) }
    LaunchedEffect(device) {
        if (pinned?.device != device) pinned = null
    }
    // The size the device would take if the handle being dragged were let go, and what it started at.
    var resizing by remember(device) { mutableStateOf<ResizeDrag?>(null) }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val fallback = density.density
        val portraitBounds = if (layoutPortrait) bounds.size else Size(bounds.height, bounds.width)
        val layoutDegrees = if (layoutPortrait) 0f else -90f
        // The handles reach half their size beyond the device and have to stay inside this view to
        // be touched, so the device is fitted to a space that much smaller all round.
        val handleRoom = if (onResize != null) with(density) { ResizeHandleSize.roundToPx() } else 0
        val fitConstraints = constraints.offset(-handleRoom, -handleRoom)
        val fitAtRest = turnedFitScale(fitConstraints, portraitBounds, layoutDegrees, fallback)
        // Pixels per logical pixel of the simulated device.
        val scale = pinned?.takeIf { it.device == device }?.let { min(it.scale, fitAtRest) } ?: fitAtRest
        // Mid-turn the device shrinks a little to stay inside the space. Only the layer is scaled,
        // so the content isn't laid out again on every frame of the turn.
        val turnModifier = Modifier.graphicsLayer {
            val degrees = turn
            val turnScale = turnedFitScale(fitConstraints, portraitBounds, layoutDegrees + degrees, fallback) / fitAtRest
            rotationZ = degrees
            scaleX = turnScale
            scaleY = turnScale
        }
        // A larger display size draws the content larger on the same screen, so it has fewer dp.
        val simulatedDensity = remember(scale, displayScale, fontScale) {
            Density(density = scale * displayScale, fontScale = fontScale)
        }
        // The window the content sees is the device's screen, so window size classes follow it.
        val hostWindowInfo = LocalWindowInfo.current
        val simulatedWindowInfo = remember(hostWindowInfo, screenSize, scale, displayScale) {
            SimulatedWindowInfo(hostWindowInfo, screenSize, scale, displayScale)
        }

        // Recorded for screenshots, which are at the device's own resolution. A freeform window has
        // none, so it gets the host's.
        val screenshots = LocalDeviceScreenshots.current
        val screenLayer = screenshots?.let { rememberGraphicsLayer() }
        val deviceLayer = screenshots?.let { rememberGraphicsLayer() }
        if (screenshots != null && screenLayer != null && deviceLayer != null) {
            val pixelRatio = if (device is FreeformDevice) density.density else device.screenSize.density
            fun pixels(size: Size) = IntSize((size.width * pixelRatio).roundToInt(), (size.height * pixelRatio).roundToInt())
            val screenPixels = pixels(screenSize)
            val devicePixels = pixels(bounds.size)
            DisposableEffect(screenshots, screenLayer, deviceLayer, screenPixels, devicePixels, isTurning) {
                // Mid-turn the layers hold a device on its way between orientations.
                if (!isTurning) {
                    screenshots.attach(
                        screen = DeviceScreenshots.Recording(screenLayer, screenPixels),
                        device = DeviceScreenshots.Recording(deviceLayer, devicePixels),
                    )
                }
                onDispose { screenshots.detach() }
            }
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
            // Inside the turn, so a screenshot of the device is upright in its orientation.
            modifier = turnModifier.then(deviceLayer?.let { Modifier.recordInto(it) } ?: Modifier).then(bodyModifier),
            content = {
                val screenCapture = screenLayer?.let { Modifier.recordInto(it) } ?: Modifier
                Box(screenCapture.then(screenModifier).then(overlayModifier).then(inputModifier)) {
                    CompositionLocalProvider(
                        LocalDensity provides simulatedDensity,
                        LocalWindowInfo provides simulatedWindowInfo,
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        // The insets keep their size on the screen, so they are fewer of the content's dp.
                        SimulatedPlatform(safeArea / displayScale, keyboardInset / displayScale, isDarkMode, content)
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

        if (onResize != null && !isTurning && layoutPortrait == wantsPortrait) {
            // The largest the device can get at the scale a drag started with, in dp.
            fun roomAt(scale: Float) = Size(
                if (fitConstraints.hasBoundedWidth) fitConstraints.maxWidth / scale else Float.POSITIVE_INFINITY,
                if (fitConstraints.hasBoundedHeight) fitConstraints.maxHeight / scale else Float.POSITIVE_INFINITY,
            )
            val resize = resizing
            resize?.let { drag ->
                ResizeOutline(resizedScreenSize(drag.start, drag.handle, drag.offset, drag.scale, roomAt(drag.scale)), drag.scale)
            }
            ResizeHandles(
                visibleSize = IntSize((bounds.width * scale).roundToInt(), (bounds.height * scale).roundToInt()),
                showCorners = device is FreeformDevice,
                showEdges = device is FreeformDevice,
                active = resize?.handle,
                onStart = { handle -> resizing = ResizeDrag(handle, screenSize, scale) },
                onDrag = { offset -> resizing = resizing?.copy(offset = offset) },
                onEnd = {
                    resizing?.let { drag ->
                        resizing = null
                        val size = resizedScreenSize(drag.start, drag.handle, drag.offset, drag.scale, roomAt(drag.scale))
                        val resized = FreeformDevice(size)
                        val isChanged = size.width.roundToInt() != drag.start.width.roundToInt() ||
                            size.height.roundToInt() != drag.start.height.roundToInt()
                        if (isChanged && resized != device) {
                            pinned = PinnedScale(resized, drag.scale)
                            onResize(resized)
                        }
                    }
                },
                onCancel = { resizing = null },
            )
        }
    }
}

/** A [scale] a resized [device] keeps while it is the one shown. */
private class PinnedScale(val device: Device, val scale: Float)

private val FreeformCornerRadius = 4.dp
private val FreeformShadow = 8.dp
private val FreeformBorder = Color(0xFF9A9AA6)

/**
 * The host's window, resized to a device screen of [screenSize] logical pixels at [scale], which
 * the content sees as [displayScale] times fewer dp.
 */
private class SimulatedWindowInfo(
    private val host: WindowInfo,
    screenSize: Size,
    scale: Float,
    displayScale: Float,
) : WindowInfo {
    override val isWindowFocused: Boolean get() = host.isWindowFocused
    override val containerSize: IntSize =
        IntSize((screenSize.width * scale).roundToInt(), (screenSize.height * scale).roundToInt())
    override val containerDpSize: DpSize = DpSize((screenSize.width / displayScale).dp, (screenSize.height / displayScale).dp)
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
