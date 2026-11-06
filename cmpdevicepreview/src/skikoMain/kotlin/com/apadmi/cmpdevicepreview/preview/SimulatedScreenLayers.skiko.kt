@file:OptIn(InternalComposeUiApi::class)

package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlin.math.max
import kotlin.math.roundToInt

// Compose Multiplatform draws a Dialog or Popup in a layer of its own over the whole window, which
// knows nothing of the device. The layers come from a scene context that is internal to Compose,
// so this file reaches past its visibility to hand Dialog and Popup one that knows the screen.
// CONTRIBUTING.md lists this and the library's other uses of Compose internals.

/**
 * Compose's own local for the scene that `Dialog` and `Popup` make their layers in, or null if the
 * Compose this runs with no longer has it. It is internal to Compose, and the only way to reach
 * those layers.
 */
private val LocalSceneContext: ProvidableCompositionLocal<ComposeSceneContext?>? = findLocalSceneContext()

/** The device's screen, as the dialogs and popups shown on it need it. */
@Stable
@Suppress("USE_DATA_CLASS")  // It holds Compose state, so there are no values to compare or copy.
internal class SimulatedScreen(
    windowInfo: WindowInfo,
    insets: PlatformWindowInsets,
    shape: Shape
) {
    /** The device's window, the size of its screen. */
    var windowInfo by mutableStateOf(windowInfo)

    /** The screen's insets, from its edges. */
    var insets by mutableStateOf(insets)

    /** The screen's outline. */
    var shape by mutableStateOf(shape)

    /** Where the screen's top-left corner is in the window, in pixels, as it is drawn: zoomed. */
    var origin by mutableStateOf(IntOffset.Zero)

    /** How many times larger the preview's zoom draws the screen than it is laid out. */
    var scale by mutableFloatStateOf(1f)

    /** The part of the screen the preview shows, in the window: a zoomed screen runs past it. */
    var visible by mutableStateOf(Rect.Zero)

    val size: IntSize get() = windowInfo.containerSize

    val bounds: IntRect get() = inWindow(size.toIntRect())

    fun inWindow(local: IntRect): IntRect = IntRect(
        left = (origin.x + local.left * scale).roundToInt(),
        top = (origin.y + local.top * scale).roundToInt(),
        right = (origin.x + local.right * scale).roundToInt(),
        bottom = (origin.y + local.bottom * scale).roundToInt(),
    )

    fun inScreen(window: IntRect): IntRect = IntRect(
        left = ((window.left - origin.x) / scale).roundToInt(),
        top = ((window.top - origin.y) / scale).roundToInt(),
        right = ((window.right - origin.x) / scale).roundToInt(),
        bottom = ((window.bottom - origin.y) / scale).roundToInt(),
    )
}

private class ScreenSceneContext(
    private val host: ComposeSceneContext,
    private val screen: SimulatedScreen,
) : ComposeSceneContext {
    override val platformContext: PlatformContext get() = host.platformContext

    override fun createLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        focusable: Boolean,
        consumePointerInputOutside: Boolean,
    ): ComposeSceneLayer =
        ScreenLayer(host.createLayer(density, layoutDirection, focusable, consumePointerInputOutside), screen, density)
}

/**
 * A layer over the whole window, drawn only within [screen].
 *
 * A dialog's content is laid out as if the screen were the window, so it is sized, centred and
 * inset within the screen, and its scrim covers only the screen. A popup is placed by an anchor
 * measured in the window, so it keeps the window, and the window around the screen is added to
 * its insets to keep it on the screen.
 */
private class ScreenLayer(
    private val layer: ComposeSceneLayer,
    private val screen: SimulatedScreen,
    density: Density,
) : ComposeSceneLayer by layer {
    // The device's density. On iOS a layer keeps the real screen's whatever it is given, so the
    // content is given it too.
    private var _density by mutableStateOf(density)

    // Drawn over the screen by the content, not over the whole window by the wrapped layer.
    private var _scrimColor by mutableStateOf<Color?>(null)

    /** A dialog's layer, rather than a popup's. Only a dialog has a scrim, set before its content. */
    private var isDialog = false

    override var density: Density
        get() = _density
        set(value) {
            _density = value
            layer.density = value
        }

    override var scrimColor: Color?
        get() = _scrimColor
        set(value) {
            _scrimColor = value
        }

    // Where a popup placed itself, before the zoom enlarges it about its top-left corner.
    private var popupBounds by mutableStateOf(IntRect.Zero)

    // A dialog's bounds are within the screen, as it is laid out; the wrapped layer has them in the
    // window, as drawn. They are set while the dialog is placed, so they are set again when the
    // screen moves or is zoomed.
    @Suppress("NO_CORRESPONDING_PROPERTY")  // It is the wrapped layer's, so there is no backing property.
    override var boundsInWindow: IntRect
        get() = if (isDialog) screen.inScreen(layer.boundsInWindow) else popupBounds
        set(value) {
            if (isDialog) {
                layer.boundsInWindow = screen.inWindow(value)
            } else {
                popupBounds = value
                layer.boundsInWindow = value.zoomedOnto(screen)
            }
        }

    override fun calculateLocalPosition(positionInWindow: IntOffset): IntOffset =
        if (isDialog) positionInWindow else layer.calculateLocalPosition(positionInWindow)

    override fun setContent(parentCompositionContext: CompositionContext, content: @Composable () -> Unit) {
        isDialog = scrimColor != null
        layer.setContent(parentCompositionContext) {
            CompositionLocalProvider(LocalDensity provides density) {
                if (isDialog) DialogOnScreen(content) else PopupOnScreen(content)
            }
        }
    }

    @Composable
    private fun DialogOnScreen(content: @Composable () -> Unit) {
        // Given again, as on iOS a layer has the real window's instead of those it was opened with.
        CompositionLocalProvider(
            LocalWindowInfo provides screen.windowInfo,
            LocalPlatformWindowInsets provides screen.insets,
        ) {
            Layout(
                content = content,
                modifier = Modifier.clipToScreen(screen, layer) { scrimColor }.windowInsets(screen.insets),
            ) { measurables, constraints ->
                val screenConstraints = Constraints(maxWidth = screen.size.width, maxHeight = screen.size.height)
                // Laid out as if the preview weren't zoomed, so zooming doesn't lay it out again.
                val placeables = measurables.map { it.measure(screenConstraints) }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val origin = layer.calculateLocalPosition(screen.origin)
                    placeables.forEach {
                        it.placeWithLayer(origin) {
                            scaleX = screen.scale
                            scaleY = screen.scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PopupOnScreen(content: @Composable () -> Unit) {
        val window = LocalWindowInfo.current.containerSize
        val scale = screen.scale
        val around = PixelInsets(
            left = screen.origin.x,
            top = screen.origin.y,
            right = window.width - screen.origin.x - (screen.size.width * scale).roundToInt(),
            bottom = window.height - screen.origin.y - (screen.size.height * scale).roundToInt(),
        )
        CompositionLocalProvider(LocalPlatformWindowInsets provides ScreenInWindowInsets(screen.insets, around, scale)) {
            Layout(content = content, modifier = Modifier.clipToScreen(screen, layer) { null }) { measurables, constraints ->
                val placeables = measurables.map { it.measure(constraints) }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    // Compose measures a popup's anchor at its zoomed position but its unzoomed size, and a
                    // popup can't be placed from in here, so under zoom its gap from the anchor is a little off.
                    placeables.forEach {
                        it.placeWithLayer(0, 0) {
                            scaleX = screen.scale
                            scaleY = screen.scale
                            transformOrigin = TransformOrigin(popupBounds.left / size.width, popupBounds.top / size.height)
                            val drawn = popupBounds.zoomedOnto(screen)
                            translationX = (drawn.left - popupBounds.left).toFloat()
                            translationY = (drawn.top - popupBounds.top).toFloat()
                        }
                    }
                }
            }
        }
    }
}

/**
 * [screen]'s insets, [scale] times as large as the zoom draws them, with the window [around] the
 * screen added to its system bars, which are what a popup keeps clear of. Its content sees the
 * screen's own insets.
 */
private data class ScreenInWindowInsets(
    private val screen: PlatformWindowInsets,
    private val around: PixelInsets,
    private val scale: Float,
) : PlatformWindowInsets {
    override val systemBars: PlatformInsets = screen.systemBars.let {
        PixelInsets(
            (it.left * scale).roundToInt() + around.left,
            (it.top * scale).roundToInt() + around.top,
            (it.right * scale).roundToInt() + around.right,
            (it.bottom * scale).roundToInt() + around.bottom,
        )
    }

    override fun excluding(safeInsets: Boolean, ime: Boolean): PlatformWindowInsets = screen.excluding(safeInsets, ime)
}

/**
 * Keeps [screen]'s origin, scale and visible part up to date. Put it on a node that fills the screen.
 *
 * @param screen The screen whose origin, scale and visible part this node sets.
 * @return This modifier, then the one that tracks the screen's position, zoom and visibility.
 */
internal fun Modifier.screenPosition(screen: SimulatedScreen): Modifier = onGloballyPositioned { coordinates ->
    val width = coordinates.size.width.toFloat()
    if (width == 0f) {
        return@onGloballyPositioned
    }
    // A distance rather than a width, so that a turn of the device doesn't skew it.
    val topEdge = coordinates.localToWindow(Offset(width, 0f)) - coordinates.localToWindow(Offset.Zero)
    val scale = topEdge.getDistance() / width
    // A turn of the device swings its corners round, so the corner is found from the centre, which
    // barely moves: a dialog stays put while the device turns under it.
    val half = Offset(width / 2f, coordinates.size.height / 2f)
    val topLeft = coordinates.localToWindow(half) - half * scale
    screen.scale = scale
    screen.origin = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt())
    screen.visible = coordinates.boundsInWindow()
}

// A popup is placed as if it weren't zoomed, so what the zoom enlarges can run off the screen's far
// edges: it is moved back, never past the screen's near ones.
private fun IntRect.zoomedOnto(screen: SimulatedScreen): IntRect {
    val enlarged = IntRect(left, top, left + (width * screen.scale).roundToInt(), top + (height * screen.scale).roundToInt())
    val onScreen = screen.bounds
    val back = IntOffset(
        (enlarged.right - max(right, onScreen.right)).coerceIn(0, max(0, enlarged.left - onScreen.left)),
        (enlarged.bottom - max(bottom, onScreen.bottom)).coerceIn(0, max(0, enlarged.top - onScreen.top)),
    )
    return enlarged.translate(-back)
}

/**
 * Clips what [layer] draws to [screen]'s outline as the zoom draws it, and to the part of it the
 * preview shows, over [scrim] filling the screen.
 */
private fun Modifier.clipToScreen(
    screen: SimulatedScreen,
    layer: ComposeSceneLayer,
    scrim: () -> Color?
): Modifier =
    drawWithCache {
        val origin = layer.calculateLocalPosition(screen.origin).toOffset()
        val visible = screen.visible.translate(layer.calculateLocalPosition(IntOffset.Zero).toOffset())
        val zoom = screen.scale
        val size = screen.size.toSize()
        val outline = Path().apply {
            addOutline(screen.shape.createOutline(size, layoutDirection, this@drawWithCache))
            transform(Matrix().apply {
                translate(origin.x, origin.y)
                scale(zoom, zoom)
            })
        }
        onDrawWithContent {
            clipRect(visible.left, visible.top, visible.right, visible.bottom) {
                clipPath(outline) {
                    scrim()?.let { drawRect(it, origin, size * zoom) }
                    this@onDrawWithContent.drawContent()
                }
            }
        }
    }

/**
 * Remembers the device's screen, updated to the latest of each of these.
 *
 * @param windowInfo The device's window, the size of its screen.
 * @param insets The screen's insets, from its edges.
 * @param shape The screen's outline.
 * @return The screen, the same one each time.
 */
@Composable
internal fun rememberSimulatedScreen(
    windowInfo: WindowInfo,
    insets: PlatformWindowInsets,
    shape: Shape
): SimulatedScreen =
    remember { SimulatedScreen(windowInfo, insets, shape) }.apply {
        this.windowInfo = windowInfo
        this.insets = insets
        this.shape = shape
    }

/**
 * Keeps the dialogs and popups opened in [content] to [screen], unless the Compose this runs with
 * has no [LocalSceneContext].
 *
 * @param screen The screen to keep them to.
 * @param content What is shown, and opens the dialogs and popups.
 */
@Composable
internal fun ScreenLayers(screen: SimulatedScreen, content: @Composable () -> Unit) {
    val local = LocalSceneContext
    if (local == null) {
        content()
        return
    }
    val host = local.current
    val context = remember(host, screen) { host?.let { ScreenSceneContext(it, screen) } }
    CompositionLocalProvider(local provides context, content = content)
}

// Why the try/catch: LocalComposeSceneContext is internal, so any Compose release may rename or
// remove it, and an app can pair this library with a newer Compose than it was built against. The
// reference is then broken when the app runs: on the JVM it throws a LinkageError (NoSuchMethodError
// or NoClassDefFoundError) the first time it is read, and on iOS and the web, Kotlin's partial
// linkage warns when the app is linked and throws here instead (unless the app has turned partial
// linkage off, when its build fails). Catching it leaves dialogs and popups as Compose draws them,
// over the whole window, rather than crashing the preview.
// It catches Throwable because the error partial linkage throws is internal to the Kotlin runtime.
// The read happens once, here, because Compose doesn't allow a try/catch around composable calls.
// This only guards the reference: a change to the interfaces this file implements, or to how
// Dialog and Popup use them, can still break the preview. SimulatedScreenLayersTest catches that
// on the JVM, and CONTRIBUTING.md says what to check on iOS.
@Suppress("INVISIBLE_REFERENCE")
private fun findLocalSceneContext(): ProvidableCompositionLocal<ComposeSceneContext?>? =
    try {
        androidx.compose.ui.scene.LocalComposeSceneContext
    } catch (_: Throwable) {
        null
    }
