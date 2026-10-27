package com.apadmi.cmpdevicepreview.preview

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
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

import com.apadmi.cmpdevicepreview.core.Appearance
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.DeviceInsets
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.hasFrameArtwork
import com.apadmi.cmpdevicepreview.core.isLaidOutInPortrait
import com.apadmi.cmpdevicepreview.core.isRotatable
import com.apadmi.cmpdevicepreview.core.isTouchscreen
import com.apadmi.cmpdevicepreview.core.keyboardHeightIn
import com.apadmi.cmpdevicepreview.core.safeAreaIn
import com.apadmi.cmpdevicepreview.core.screenSizeIn
import com.apadmi.cmpdevicepreview.core.turned
import com.apadmi.cmpdevicepreview.frame.DeviceFramePainter
import com.apadmi.cmpdevicepreview.frame.DeviceScreenShape
import com.apadmi.cmpdevicepreview.frame.SystemUiColors
import com.apadmi.cmpdevicepreview.frame.SystemUiPainter
import com.apadmi.cmpdevicepreview.frame.paintSimulatedKeyboard
import com.apadmi.cmpdevicepreview.frame.paintSystemUi

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** How long the device takes to turn between portrait and landscape. */
internal const val TurnMillis = 450

private val WindowCornerRadius = 4.dp
private val WindowShadow = 8.dp
private val WindowShape = RoundedCornerShape(WindowCornerRadius)

/** Gets the device a drag resized to, and the scale the drag started at. */
private typealias OnResized = (resized: FreeformDevice, scale: Float) -> Unit

/**
 * The orientation the content is laid out in, and how far the device is turned from it. A change
 * of orientation turns the device a quarter turn first, as a phone being turned does, and only
 * then lays the content out in the new orientation.
 */
@Stable
@Suppress("USE_DATA_CLASS")  // It holds Compose state, so there are no values to compare or copy.
private class DeviceTurn(isPortrait: Boolean) {
    var isLayoutPortrait by mutableStateOf(isPortrait)

    /** Degrees clockwise from [isLayoutPortrait]. Read it while drawing, not composing. */
    var degrees by mutableFloatStateOf(0f)
    val isTurning: Boolean by derivedStateOf { degrees != 0f }
}

/**
 * The graphics layers the screenshots of a device are recorded into.
 *
 * @property screen The layer for the screen alone.
 * @property device The layer for the whole device, in its frame.
 */
private class ScreenshotLayers(val screen: GraphicsLayer, val device: GraphicsLayer)

/**
 * A [scale] a resized [device] keeps while it is the one shown.
 *
 * @property device The device the scale is kept for.
 * @property scale The kept scale, in pixels per dp.
 */
private class PinnedScale(val device: Device, val scale: Float)

/** The host's window, resized to the device's screen. */
private class SimulatedWindowInfo(
    private val host: WindowInfo,
    screenSize: Size,
    scale: Float,
    displayScale: Float,
) : WindowInfo {
    override val isWindowFocused: Boolean get() = host.isWindowFocused
    override val containerSize: IntSize = screenSize.toPixels(scale)
    override val containerDpSize: DpSize = DpSize((screenSize.width / displayScale).dp, (screenSize.height / displayScale).dp)
}

/**
 * Draws the keyboard band and the system bars over the content. The bars use the device's safe
 * area, so the gesture pill stays on top of the keyboard band, and its layout direction, so a
 * phone in RTL mirrors its status bar.
 */
private fun Modifier.systemOverlay(
    systemUi: SystemUiPainter?,
    keyboardInset: Float,
    screenSize: Size,
    safeArea: DeviceInsets,
    isDarkMode: Boolean,
    deviceLayoutDirection: LayoutDirection,
): Modifier {
    if (systemUi == null && keyboardInset <= 0f) {
        return this
    }
    val iconColor = SystemUiColors.icons(isDarkMode)
    return drawWithCache {
        val bars = systemUi?.layout(screenSize, safeArea, deviceLayoutDirection).orEmpty()
        onDrawWithContent {
            drawContent()
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.scale(size.width / screenSize.width, size.height / screenSize.height)
                paintSimulatedKeyboard(canvas, screenSize, keyboardInset)
                paintSystemUi(canvas, bars, iconColor)
                canvas.restore()
            }
        }
    }
}

/** Draws the body artwork behind the screen, for a device laid out at [scale] within [bounds]. */
private fun Modifier.deviceBody(
    bodyPainter: DeviceFramePainter?,
    bounds: Rect,
    scale: Float,
    screenSize: Size,
    isPortrait: Boolean,
): Modifier {
    if (bodyPainter == null) {
        return this
    }
    return drawBehind {
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.scale(scale, scale)
            canvas.translate(-bounds.left, -bounds.top)
            bodyPainter.paintBody(canvas, screenSize, isPortrait)
            canvas.restore()
        }
    }
}

/**
 * The resize handles, and while one is dragged, an outline of the size the device will take. The
 * device isn't laid out again until the handle is let go, when [onResize] gets the new window and
 * the scale the drag started at.
 */
@Composable
private fun BoxScope.ResizeLayer(
    device: Device,
    screenSize: Size,
    visibleSize: IntSize,
    scale: Float,
    room: Constraints,
    onResize: OnResized,
) {
    var resizing by remember(device) { mutableStateOf<ResizeDrag?>(null) }

    resizing?.let { drag -> ResizeOutline(sizeAfter(drag, room), drag.scale) }
    ResizeHandles(
        visibleSize = visibleSize,
        isFreeform = device is FreeformDevice,
        active = resizing?.handle,
        onStart = { handle -> resizing = ResizeDrag(handle, screenSize, scale) },
        onDrag = { offset -> resizing = resizing?.copy(offset = offset) },
        onEnd = {
            resizing?.let { drag ->
                resizing = null
                val size = sizeAfter(drag, room)
                val isChanged = size.width.roundToInt() != drag.start.width.roundToInt() ||
                        size.height.roundToInt() != drag.start.height.roundToInt()
                val resized = FreeformDevice(size)
                if (isChanged && resized != device) {
                    onResize(resized, drag.scale)
                }
            }
        },
        onCancel = { resizing = null },
    )
}

/** This size in pixels, at [scale] pixels per unit. */
private fun Size.toPixels(scale: Float): IntSize = IntSize((width * scale).roundToInt(), (height * scale).roundToInt())

/** Clips the content to [shape], bordered and shadowed when it is a plain window's. */
private fun Modifier.screenClip(shape: Shape): Modifier =
    if (shape === WindowShape) {
        shadow(WindowShadow, shape, clip = false)
            .border(shape = shape, border = BorderStroke(1.dp, PanelColors.FrameOutline))
            .clip(shape)
    } else {
        clip(shape)
    }

/**
 * Draws [device] around [content], shown as [configuration] describes, scaled to fit the space.
 *
 * The content is laid out at the device's screen size in dp. Inside it, `WindowInsets`,
 * `isSystemInDarkTheme()`, `LocalDensity` and `LocalWindowInfo` describe the device, except on
 * Android, where insets and theme stay the real window's. Dialogs and popups are kept to the
 * screen, except on Android, where each is a window of its own. The status bar, gesture pill and
 * keyboard band are drawn over the content and don't take touches. On the web, the mouse acts
 * as a finger on a phone, tablet, foldable or freeform window.
 *
 * @param device The device to draw.
 * @param modifier The modifier for the view, which the device is scaled to fit.
 * @param configuration How the device is shown, such as its orientation, theme and scale.
 * @param onResize When set, dragging a corner of the device (or an edge of a [FreeformDevice])
 * resizes it into a [FreeformDevice], passed here on release. Pass it back as [device] to apply it.
 * @param content What the device's screen shows.
 */
@Composable
public fun DeviceFrameView(
    device: Device,
    modifier: Modifier = Modifier,
    configuration: DeviceConfiguration = DeviceConfiguration(),
    onResize: ((FreeformDevice) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    DeviceFrame(device, modifier, configuration, device.isTouchscreen, onResize, onLayoutScale = null, content)
}

@Composable
internal fun DeviceFrame(
    device: Device,
    modifier: Modifier,
    configuration: DeviceConfiguration,
    isMouseAsTouch: Boolean,
    onResize: ((FreeformDevice) -> Unit)?,
    onLayoutScale: ((Float) -> Unit)?,
    content: @Composable () -> Unit,
) {
    val wantsPortrait = device.isLaidOutInPortrait(configuration.orientation)
    val turn = rememberDeviceTurn(device, wantsPortrait)
    val isPortrait = turn.isLayoutPortrait
    val screenSize = device.screenSizeIn(isPortrait)
    val safeArea = device.safeAreaIn(isPortrait)
    val keyboardInset = if (configuration.isKeyboardVisible) device.keyboardHeightIn(isPortrait) ?: 0f else 0f
    val layoutDirection = configuration.layoutDirection ?: LocalLayoutDirection.current

    val painter = remember(device.frame) { device.frame?.let(::DeviceFramePainter) }
    val bodyPainter = painter?.takeIf { configuration.isFrameVisible && it.body != null }
    // What has to stay visible, in screen coordinates: the whole body when it is drawn.
    val bounds = bodyPainter?.frame?.bodyBounds(screenSize, isPortrait) ?: Rect(Offset.Zero, screenSize)
    val systemUiPainter = remember(device.systemUi) {
        device.systemUi?.takeUnless { it.isEmpty }?.let(::SystemUiPainter)
    }.takeIf { configuration.isSystemUiVisible }

    val screenShape = rememberScreenShape(painter, screenSize, isPortrait, isWindow = !device.hasFrameArtwork, configuration.isFrameVisible)
    val screenModifier = Modifier.screenClip(screenShape)
        .systemOverlay(systemUiPainter, keyboardInset, screenSize, safeArea, configuration.appearance == Appearance.Dark, layoutDirection)
        .then(if (isMouseAsTouch) Modifier.mouseAsTouch() else Modifier)

    // The scale a resized device keeps instead of fitting the space again. Forgotten once another
    // device is shown, so coming back to the same size fits the space again.
    var pinned by remember { mutableStateOf<PinnedScale?>(null) }
    LaunchedEffect(device) {
        if (pinned?.device != device) {
            pinned = null
        }
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val portraitBounds = if (isPortrait) bounds.size else bounds.size.turned()
        val layoutDegrees = if (isPortrait) 0f else -90f
        // The handles reach half their size beyond the device and have to stay inside this view to
        // be touched, so the device is fitted to a space that much smaller all round.
        val handleRoom = if (onResize != null) with(density) { ResizeHandleSize.roundToPx() } else 0
        val fitConstraints = constraints.offset(-handleRoom, -handleRoom)
        val fitAtRest = turnedFitScale(fitConstraints, portraitBounds, layoutDegrees, density.density)
        // Pixels per dp of the simulated device. A pin for another device is ignored until the effect
        // above clears it.
        val scale = pinned?.takeIf { it.device == device }?.let { min(it.scale, fitAtRest) } ?: fitAtRest
        SideEffect { onLayoutScale?.invoke(scale) }
        // Mid-turn the device shrinks a little to stay inside the space. Only the layer is scaled,
        // so the content isn't laid out again on every frame of the turn.
        val turnModifier = Modifier.graphicsLayer {
            val degrees = turn.degrees
            val turnScale = turnedFitScale(fitConstraints, portraitBounds, layoutDegrees + degrees, density.density) / fitAtRest
            rotationZ = degrees
            scaleX = turnScale
            scaleY = turnScale
        }
        val simulatedDensity = remember(scale, configuration.displayScale, configuration.fontScale) {
            Density(density = scale * configuration.displayScale, fontScale = configuration.fontScale)
        }
        // So that window size classes follow the device's screen rather than the host's window.
        val hostWindowInfo = LocalWindowInfo.current
        val simulatedWindowInfo = remember(hostWindowInfo, screenSize, scale, configuration.displayScale) {
            SimulatedWindowInfo(hostWindowInfo, screenSize, scale, configuration.displayScale)
        }
        val screenshotLayers = recordScreenshots(device, screenSize, bounds.size, turn.isTurning)
        val visibleSize = bounds.size.toPixels(scale)

        Layout(
            // Inside the turn, so a screenshot of the device is upright in its orientation.
            modifier = turnModifier
                .then(screenshotLayers?.device?.let { Modifier.recordInto(it) } ?: Modifier)
                .deviceBody(bodyPainter, bounds, scale, screenSize, isPortrait),
            content = {
                val screenCapture = screenshotLayers?.screen?.let { Modifier.recordInto(it) } ?: Modifier
                Box(screenCapture.then(screenModifier)) {
                    CompositionLocalProvider(
                        LocalDensity provides simulatedDensity,
                        LocalWindowInfo provides simulatedWindowInfo,
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        SimulatedPlatform(
                            safeArea / configuration.displayScale,
                            keyboardInset / configuration.displayScale,
                            configuration.appearance == Appearance.Dark,
                            screenShape,
                        ) {
                            ProvideLanguage(configuration.language, content)
                        }
                    }
                }
            },
        ) { measurables, _ ->
            val screenPixels = screenSize.toPixels(scale)
            val screen = measurables.single().measure(Constraints.fixed(screenPixels.width, screenPixels.height))
            layout(visibleSize.width, visibleSize.height) {
                screen.place((-bounds.left * scale).roundToInt(), (-bounds.top * scale).roundToInt())
            }
        }

        if (onResize != null && !turn.isTurning && isPortrait == wantsPortrait) {
            ResizeLayer(
                device = device,
                screenSize = screenSize,
                visibleSize = visibleSize,
                scale = scale,
                room = fitConstraints,
                onResize = { resized, dragScale ->
                    pinned = PinnedScale(resized, dragScale)
                    onResize(resized)
                },
            )
        }
    }
}

/**
 * The scale at which a device whose portrait bounds are [portraitBounds] fits [constraints] when
 * turned [degrees] clockwise from portrait. Never larger than in portrait: turning a device doesn't
 * make it bigger, though it shrinks when it would otherwise not fit.
 *
 * @param constraints The space the device has to fit.
 * @param portraitBounds The device's bounds in portrait, in dp.
 * @param degrees How far the device is turned clockwise from portrait.
 * @param fallback The scale to use when [constraints] doesn't bound the space.
 * @return The scale, in pixels per dp.
 */
internal fun turnedFitScale(
    constraints: Constraints,
    portraitBounds: Size,
    degrees: Float,
    fallback: Float
): Float {
    val radians = degrees * PI.toFloat() / 180f
    val cos = abs(cos(radians))
    val sin = abs(sin(radians))
    val turned = Size(
        width = portraitBounds.width * cos + portraitBounds.height * sin,
        height = portraitBounds.width * sin + portraitBounds.height * cos,
    )
    return min(fitScale(constraints, portraitBounds, fallback), fitScale(constraints, turned, fallback))
}

@Composable
private fun rememberDeviceTurn(device: Device, wantsPortrait: Boolean): DeviceTurn {
    // Swapping a device that can turn for one that can't (or back) starts over rather than turning.
    val turn = remember(device.isRotatable) { DeviceTurn(wantsPortrait) }
    LaunchedEffect(turn, wantsPortrait) {
        val target = when {
            wantsPortrait == turn.isLayoutPortrait -> 0f
            // Landscape is portrait turned counter-clockwise.
            wantsPortrait -> 90f
            else -> -90f
        }
        if (turn.degrees != target) {
            animate(turn.degrees, target, animationSpec = tween(TurnMillis, easing = FastOutSlowInEasing)) { value, _ ->
                turn.degrees = value
            }
        }
        if (wantsPortrait != turn.isLayoutPortrait) {
            turn.isLayoutPortrait = wantsPortrait
            turn.degrees = 0f
        }
    }
    return turn
}

/**
 * The outline of the device's screen. A device without artwork is a plain window, with rounded
 * corners while its frame is shown.
 */
@Composable
private fun rememberScreenShape(
    painter: DeviceFramePainter?,
    screenSize: Size,
    isPortrait: Boolean,
    isWindow: Boolean,
    isFrameVisible: Boolean,
): Shape {
    val screenShape = remember(painter, screenSize, isPortrait) {
        painter?.screenClip(screenSize, isPortrait)?.let { DeviceScreenShape(it, screenSize) }
    }
    return when {
        screenShape != null -> screenShape
        isWindow && isFrameVisible -> WindowShape
        else -> RectangleShape
    }
}

/**
 * Records the screen and the whole device for [LocalDeviceScreenshots], at the device's own
 * resolution (the host's for a device without one), except mid-turn. Null when nothing records.
 */
@Composable
private fun recordScreenshots(
    device: Device,
    screenSize: Size,
    deviceSize: Size,
    isTurning: Boolean
): ScreenshotLayers? {
    val screenshots = LocalDeviceScreenshots.current ?: return null
    val screenLayer = rememberGraphicsLayer()
    val deviceLayer = rememberGraphicsLayer()
    val pixelRatio = device.devicePixelRatio ?: LocalDensity.current.density
    val screenPixels = screenSize.toPixels(pixelRatio)
    val devicePixels = deviceSize.toPixels(pixelRatio)
    DisposableEffect(screenshots, screenLayer, deviceLayer, screenPixels, devicePixels, isTurning) {
        if (!isTurning) {
            screenshots.attach(
                screen = DeviceScreenshots.Recording(screenLayer, screenPixels),
                device = DeviceScreenshots.Recording(deviceLayer, devicePixels),
            )
        }
        onDispose { screenshots.detach() }
    }
    return remember(screenLayer, deviceLayer) { ScreenshotLayers(screenLayer, deviceLayer) }
}

/** The largest the device can get at the scale a drag started with, in dp, within [room]. */
private fun roomAt(room: Constraints, dragScale: Float) = Size(
    if (room.hasBoundedWidth) room.maxWidth / dragScale else Float.POSITIVE_INFINITY,
    if (room.hasBoundedHeight) room.maxHeight / dragScale else Float.POSITIVE_INFINITY,
)

/** The screen size [drag] would resize the device to, within [room]. */
private fun sizeAfter(drag: ResizeDrag, room: Constraints) =
    resizedScreenSize(drag.start, drag.handle, drag.offset, drag.scale, roomAt(room, drag.scale))

/** The largest scale at which [content] fits [constraints], or [fallback] when unbounded. */
private fun fitScale(
    constraints: Constraints,
    content: Size,
    fallback: Float
): Float {
    val horizontal = if (constraints.hasBoundedWidth) constraints.maxWidth / content.width else Float.POSITIVE_INFINITY
    val vertical = if (constraints.hasBoundedHeight) constraints.maxHeight / content.height else Float.POSITIVE_INFINITY
    val scale = min(horizontal, vertical)
    return if (scale.isFinite() && scale > 0f) scale else fallback
}
