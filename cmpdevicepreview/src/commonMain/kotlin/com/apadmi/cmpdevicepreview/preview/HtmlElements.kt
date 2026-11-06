@file:Suppress("FILE_NAME_MATCH_CLASS")

// Named for the feature, like its platform files.

package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.unit.toSize
import com.apadmi.cmpdevicepreview.svg.Affine
import com.apadmi.cmpdevicepreview.svg.toCssClipPath

// A clip with nothing inside, which also keeps the pointer off the elements.
internal const val HiddenClipPath = "inset(50%)"

internal interface HtmlElementsContainer {
    // Composed in the device so that it finds the container the content's elements are put in.
    @Composable
    fun Marker()

    // Null leaves the container as Compose made it.
    fun restyle(style: HtmlElementsStyle?)
}

/**
 * The CSS the container of the content's HTML elements is given.
 *
 * @property transform The container's `transform`.
 * @property clipPath The container's `clip-path`.
 */
internal data class HtmlElementsStyle(val transform: String, val clipPath: String)

/**
 * The device's screen, which the content's HTML elements are kept to.
 *
 * @property size The screen's size, in screen units.
 * @property shape The screen's outline.
 * @property covers What the system UI and the keyboard draw over the content, in screen units.
 * @property simulatedDensity The density the content is laid out at.
 * @property isTurning Whether the device is turning, when the elements are hidden.
 */
internal data class HtmlElementsScreen(
    val size: Size,
    val shape: Shape,
    val covers: List<Rect>,
    val simulatedDensity: Float,
    val isTurning: Boolean,
)

/**
 * The element for [htmlElementsOnScreen].
 *
 * @property container The container to style.
 * @property screen The screen the elements are kept to.
 */
private data class HtmlElementsOnScreenElement(
    val container: HtmlElementsContainer,
    val screen: HtmlElementsScreen,
) : ModifierNodeElement<HtmlElementsOnScreenNode>() {
    override fun create(): HtmlElementsOnScreenNode = HtmlElementsOnScreenNode(container, screen)

    override fun update(node: HtmlElementsOnScreenNode) = node.update(container, screen)
}

private class HtmlElementsOnScreenNode(
    container: HtmlElementsContainer,
    screen: HtmlElementsScreen,
) : Modifier.Node(), GlobalPositionAwareModifierNode {
    private var container = container
    private var screen = screen
    private var coordinates: LayoutCoordinates? = null

    fun update(container: HtmlElementsContainer, screen: HtmlElementsScreen) {
        if (container !== this.container) {
            this.container.restyle(null)
            this.container = container
        }
        this.screen = screen
        restyle()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        restyle()
    }

    override fun onDetach() {
        coordinates = null
        container.restyle(null)
    }

    private fun restyle() {
        val coordinates = coordinates?.takeIf { it.isAttached } ?: return
        val size = coordinates.size.toSize()
        if (size.isEmpty()) {
            return
        }
        val visible = if (screen.isTurning) {
            null
        } else {
            uncoveredScreen(screen.shape.createOutline(size, requireLayoutDirection(), requireDensity()), screen.covers, screen.size, size)
        }
        container.restyle(htmlElementsStyle(visible, coordinates.toWindow(size), screen.simulatedDensity, requireDensity().density))
    }
}

internal fun Modifier.htmlElementsOnScreen(container: HtmlElementsContainer, screen: HtmlElementsScreen): Modifier =
    this then HtmlElementsOnScreenElement(container, screen)

// The screen is upright whenever its elements are shown, so two corners give the whole transform.
private fun LayoutCoordinates.toWindow(size: Size): Affine {
    val topLeft = localToWindow(Offset.Zero)
    val bottomRight = localToWindow(Offset(size.width, size.height))
    return Affine(
        a = (bottomRight.x - topLeft.x) / size.width,
        b = 0f,
        c = 0f,
        d = (bottomRight.y - topLeft.y) / size.height,
        e = topLeft.x,
        f = topLeft.y,
    )
}

// Null where HTML elements aren't laid over the canvas by the browser: everywhere but the web.
@Composable
internal expect fun rememberHtmlElementsContainer(): HtmlElementsContainer?

internal fun uncoveredScreen(
    outline: Outline,
    covers: List<Rect>,
    screenSize: Size,
    size: Size
): Path {
    val scaleX = size.width / screenSize.width
    val scaleY = size.height / screenSize.height
    // One at a time, so where covers overlap, such as the pill over the keyboard, is only cut once.
    return covers.fold(Path().apply { addOutline(outline) }) { visible, cover ->
        val covered = Path().apply {
            addRect(Rect(cover.left * scaleX, cover.top * scaleY, cover.right * scaleX, cover.bottom * scaleY))
        }
        val uncovered = Path()
        if (uncovered.op(visible, covered, PathOperation.Difference)) uncovered else visible
    }
}

internal fun htmlElementsStyle(
    visible: Path?,
    toWindow: Affine,
    simulatedDensity: Float,
    hostDensity: Float
): HtmlElementsStyle {
    // Compose places each element at its window pixels over the density it's composed at, not the host's.
    val scale = "scale(${simulatedDensity / hostDensity})"
    // In the container's units, which that scale makes window pixels over the simulated density.
    val toContainer = (Affine.scale(1f / simulatedDensity) * toWindow).toMatrix()
    val clipPath = visible?.let {
        Path().apply {
            addPath(it)
            fillType = it.fillType
            transform(toContainer)
        }.toCssClipPath()
    } ?: HiddenClipPath
    return HtmlElementsStyle(scale, clipPath)
}
