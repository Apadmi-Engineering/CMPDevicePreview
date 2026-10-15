@file:OptIn(InternalComposeUiApi::class)

package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
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
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
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

    /** Where the screen's top-left corner is in the window, in pixels. */
    var origin by mutableStateOf(IntOffset.Zero)

    val size: IntSize get() = windowInfo.containerSize
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

    // A dialog's bounds are within the screen, as it is laid out; the wrapped layer has them in the
    // window. They are set while the dialog is placed, so they are set again when the screen moves.
    @Suppress("NO_CORRESPONDING_PROPERTY")  // It is the wrapped layer's, so there is no backing property.
    override var boundsInWindow: IntRect
        get() = layer.boundsInWindow.let { if (isDialog) it.translate(-screen.origin) else it }
        set(value) {
            layer.boundsInWindow = value.let { if (isDialog) it.translate(screen.origin) else it }
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
                val placeables = measurables.map { it.measure(screenConstraints) }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val origin = layer.calculateLocalPosition(screen.origin)
                    placeables.forEach { it.place(origin) }
                }
            }
        }
    }

    @Composable
    private fun PopupOnScreen(content: @Composable () -> Unit) {
        val window = LocalWindowInfo.current.containerSize
        val around = PixelInsets(
            left = screen.origin.x,
            top = screen.origin.y,
            right = window.width - screen.origin.x - screen.size.width,
            bottom = window.height - screen.origin.y - screen.size.height,
        )
        CompositionLocalProvider(LocalPlatformWindowInsets provides ScreenInWindowInsets(screen.insets, around)) {
            Layout(content = content, modifier = Modifier.clipToScreen(screen, layer) { null }) { measurables, constraints ->
                val placeables = measurables.map { it.measure(constraints) }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeables.forEach { it.place(0, 0) }
                }
            }
        }
    }
}

/**
 * [screen]'s insets with the window [around] the screen added to its system bars, which are what a
 * popup keeps clear of. Its content sees the screen's own insets.
 */
private data class ScreenInWindowInsets(
    private val screen: PlatformWindowInsets,
    private val around: PixelInsets,
) : PlatformWindowInsets {
    override val systemBars: PlatformInsets = screen.systemBars.let {
        PixelInsets(it.left + around.left, it.top + around.top, it.right + around.right, it.bottom + around.bottom)
    }

    override fun excluding(safeInsets: Boolean, ime: Boolean): PlatformWindowInsets = screen.excluding(safeInsets, ime)
}

/**
 * Keeps [screen]'s origin up to date. Put it on a node that fills the screen.
 *
 * @param screen The screen whose origin this node sets.
 * @return This modifier, then the one that tracks the screen's position.
 */
internal fun Modifier.screenPosition(screen: SimulatedScreen): Modifier = onGloballyPositioned { coordinates ->
    // A turn of the device swings its corners round, so the corner is found from the centre, which
    // barely moves: a dialog stays put while the device turns under it.
    val half = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
    val topLeft = coordinates.localToWindow(half) - half
    screen.origin = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt())
}

/** Clips what [layer] draws to [screen]'s outline, over [scrim] filling the screen. */
private fun Modifier.clipToScreen(
    screen: SimulatedScreen,
    layer: ComposeSceneLayer,
    scrim: () -> Color?
): Modifier =
    drawWithCache {
        val origin = layer.calculateLocalPosition(screen.origin).toOffset()
        val size = screen.size.toSize()
        val outline = Path().apply {
            addOutline(screen.shape.createOutline(size, layoutDirection, this@drawWithCache))
            translate(origin)
        }
        onDrawWithContent {
            clipPath(outline) {
                scrim()?.let { drawRect(it, origin, size) }
                this@onDrawWithContent.drawContent()
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
