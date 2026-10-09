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
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.DeviceInsets
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.div
import com.apadmi.cmpdevicepreview.core.hasFrameArtwork
import com.apadmi.cmpdevicepreview.core.isRotatable
import com.apadmi.cmpdevicepreview.core.isTouchscreen
import com.apadmi.cmpdevicepreview.core.keyboardHeightIn
import com.apadmi.cmpdevicepreview.core.laidOutInPortrait
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
 * Draws [device] around [content], shown as [configuration] describes, scaled to fit the space.
 *
 * The content is laid out at the device's screen size in dp. Inside it, `WindowInsets`,
 * `isSystemInDarkTheme()`, `LocalDensity` and `LocalWindowInfo` describe the device, except on
 * Android, where insets and theme stay the real window's. The status bar, gesture pill and keyboard
 * band are drawn over the content and don't take touches. On the web, the mouse acts as a finger on
 * a touchscreen device.
 *
 * [DeviceConfiguration.backdrop], [DeviceConfiguration.isDotGridVisible] and
 * [DeviceConfiguration.isPreviewModeEnabled] are not used here.
 *
 * @param onResize When set, dragging a corner of the device (or an edge of a [FreeformDevice])
 * resizes it into a [FreeformDevice], passed here on release. Pass it back as [device] to apply it.
 */
@Composable
public fun DeviceFrameView(
    device: Device,
    modifier: Modifier = Modifier,
    configuration: DeviceConfiguration = DeviceConfiguration(),
    onResize: ((FreeformDevice) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val wantsPortrait = device.laidOutInPortrait(configuration.isPortrait)
    val turn = rememberDeviceTurn(device, wantsPortrait)
    val isPortrait = turn.isLayoutPortrait
    val screenSize = device.portraitSize.let { if (isPortrait) it else Size(it.height, it.width) }
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

    val screenModifier = rememberScreenClip(painter, screenSize, isPortrait, isWindow = !device.hasFrameArtwork, configuration.isFrameVisible)
        .systemOverlay(systemUiPainter, keyboardInset, screenSize, safeArea, configuration.isDarkMode, layoutDirection)
        .then(if (device.isTouchscreen) Modifier.mouseAsTouch() else Modifier)

    // The scale a resized device keeps instead of fitting the space again. Forgotten once another
    // device is shown, so coming back to the same size fits the space again.
    var pinned by remember { mutableStateOf<PinnedScale?>(null) }
    LaunchedEffect(device) {
        if (pinned?.device != device) pinned = null
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val portraitBounds = if (isPortrait) bounds.size else Size(bounds.height, bounds.width)
        val layoutDegrees = if (isPortrait) 0f else -90f
        // The handles reach half their size beyond the device and have to stay inside this view to
        // be touched, so the device is fitted to a space that much smaller all round.
        val handleRoom = if (onResize != null) with(density) { ResizeHandleSize.roundToPx() } else 0
        val fitConstraints = constraints.offset(-handleRoom, -handleRoom)
        val fitAtRest = turnedFitScale(fitConstraints, portraitBounds, layoutDegrees, density.density)
        // Pixels per dp of the simulated device. A pin for another device is ignored until the effect
        // above clears it.
        val scale = pinned?.takeIf { it.device == device }?.let { min(it.scale, fitAtRest) } ?: fitAtRest
        // Mid-turn the device shrinks a little to stay inside the space. Only the layer is scaled,
        // so the content isn't laid out again on every frame of the turn.
        val turnModifier = Modifier.graphicsLayer {
            val degrees = turn.degrees
            val turnScale = turnedFitScale(fitConstraints, portraitBounds, layoutDegrees + degrees, density.density) / fitAtRest
            rotationZ = degrees
            scaleX = turnScale
            scaleY = turnScale
        }
        // A larger display size draws the content larger on the same screen, so it has fewer dp.
        val simulatedDensity = remember(scale, configuration.displayScale, configuration.fontScale) {
            Density(density = scale * configuration.displayScale, fontScale = configuration.fontScale)
        }
        // The window the content sees is the device's screen, so window size classes follow it.
        val hostWindowInfo = LocalWindowInfo.current
        val simulatedWindowInfo = remember(hostWindowInfo, screenSize, scale, configuration.displayScale) {
            SimulatedWindowInfo(hostWindowInfo, screenSize, scale, configuration.displayScale)
        }
        val screenshotLayers = recordScreenshots(device, screenSize, bounds.size, turn.isTurning)

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
                        // The insets keep their size on the screen, so they are fewer of the content's dp.
                        SimulatedPlatform(
                            safeArea / configuration.displayScale,
                            keyboardInset / configuration.displayScale,
                            configuration.isDarkMode,
                            content,
                        )
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

        if (onResize != null && !turn.isTurning && isPortrait == wantsPortrait) {
            ResizeLayer(
                device = device,
                screenSize = screenSize,
                visibleSize = IntSize((bounds.width * scale).roundToInt(), (bounds.height * scale).roundToInt()),
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

/** How long the device takes to turn between portrait and landscape. */
private const val TurnMillis = 450

/**
 * The orientation the content is laid out in, and how far the device is turned from it. A change
 * of orientation turns the device a quarter turn first, as a phone being turned does, and only
 * then lays the content out in the new orientation.
 */
@Stable
private class DeviceTurn(isPortrait: Boolean) {
    var isLayoutPortrait by mutableStateOf(isPortrait)

    /** Degrees clockwise from [isLayoutPortrait]. Read it while drawing, not composing. */
    var degrees by mutableFloatStateOf(0f)

    val isTurning: Boolean by derivedStateOf { degrees != 0f }
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

private val WindowCornerRadius = 4.dp
private val WindowShadow = 8.dp
private val WindowBorder = Color(0xFF9A9AA6)

/**
 * Clips the content to the device's screen outline. A device without artwork is a plain window,
 * bordered and shadowed while its frame is shown.
 */
@Composable
private fun rememberScreenClip(
    painter: DeviceFramePainter?,
    screenSize: Size,
    isPortrait: Boolean,
    isWindow: Boolean,
    isFrameVisible: Boolean,
): Modifier {
    val screenShape = remember(painter, screenSize, isPortrait) {
        painter?.screenClip(screenSize, isPortrait)?.let { DeviceScreenShape(it, screenSize) }
    }
    return when {
        screenShape != null -> Modifier.clip(screenShape)
        isWindow && isFrameVisible -> RoundedCornerShape(WindowCornerRadius).let { shape ->
            Modifier
                .shadow(WindowShadow, shape, clip = false)
                .border(shape = shape, border = BorderStroke(1.dp, WindowBorder))
                .clip(shape)
        }
        else -> Modifier.clipToBounds()
    }
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
    if (systemUi == null && keyboardInset <= 0f) return this
    val iconColor = SystemUiColors.icons(isDarkMode)
    return drawWithContent {
        drawContent()
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.scale(size.width / screenSize.width, size.height / screenSize.height)
            paintSimulatedKeyboard(canvas, screenSize, keyboardInset)
            systemUi?.paint(canvas, screenSize, safeArea, iconColor, deviceLayoutDirection)
            canvas.restore()
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
    if (bodyPainter == null) return this
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

private class ScreenshotLayers(val screen: GraphicsLayer, val device: GraphicsLayer)

/**
 * Records the screen and the whole device for [LocalDeviceScreenshots], at the device's own
 * resolution (the host's for a device without one), except mid-turn. Null when nothing records.
 */
@Composable
private fun recordScreenshots(device: Device, screenSize: Size, deviceSize: Size, isTurning: Boolean): ScreenshotLayers? {
    val screenshots = LocalDeviceScreenshots.current ?: return null
    val screenLayer = rememberGraphicsLayer()
    val deviceLayer = rememberGraphicsLayer()
    val pixelRatio = device.devicePixelRatio ?: LocalDensity.current.density
    fun pixels(size: Size) = IntSize((size.width * pixelRatio).roundToInt(), (size.height * pixelRatio).roundToInt())
    val screenPixels = pixels(screenSize)
    val devicePixels = pixels(deviceSize)
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

/** A [scale] a resized [device] keeps while it is the one shown. */
private class PinnedScale(val device: Device, val scale: Float)

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
    onResize: (resized: FreeformDevice, scale: Float) -> Unit,
) {
    var resizing by remember(device) { mutableStateOf<ResizeDrag?>(null) }
    // The largest the device can get at the scale a drag started with, in dp.
    fun roomAt(dragScale: Float) = Size(
        if (room.hasBoundedWidth) room.maxWidth / dragScale else Float.POSITIVE_INFINITY,
        if (room.hasBoundedHeight) room.maxHeight / dragScale else Float.POSITIVE_INFINITY,
    )
    fun sizeAfter(drag: ResizeDrag) = resizedScreenSize(drag.start, drag.handle, drag.offset, drag.scale, roomAt(drag.scale))

    resizing?.let { drag -> ResizeOutline(sizeAfter(drag), drag.scale) }
    val isFreeform = device is FreeformDevice
    ResizeHandles(
        visibleSize = visibleSize,
        showCorners = isFreeform,
        showEdges = isFreeform,
        active = resizing?.handle,
        onStart = { handle -> resizing = ResizeDrag(handle, screenSize, scale) },
        onDrag = { offset -> resizing = resizing?.copy(offset = offset) },
        onEnd = {
            resizing?.let { drag ->
                resizing = null
                val size = sizeAfter(drag)
                val isChanged = size.width.roundToInt() != drag.start.width.roundToInt() ||
                    size.height.roundToInt() != drag.start.height.roundToInt()
                val resized = FreeformDevice(size)
                if (isChanged && resized != device) onResize(resized, drag.scale)
            }
        },
        onCancel = { resizing = null },
    )
}

/**
 * The host's window, resized to a device screen of [screenSize] dp at [scale], which the content
 * sees as [displayScale] times fewer dp.
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

/**
 * The scale at which a device whose portrait bounds are [portraitBounds] fits [constraints] when
 * turned [degrees] clockwise from portrait. Never larger than in portrait: turning a device doesn't
 * make it bigger, though it shrinks when it would otherwise not fit.
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
