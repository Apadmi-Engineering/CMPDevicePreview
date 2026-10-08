package com.apadmi.devicepreview.preview

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement

/**
 * Makes the host's mouse act as a finger on this node, as it would on a touchscreen: Compose's
 * scrollables ignore mouse drags, so without it a list only scrolls with the wheel.
 *
 * A press of the primary button that starts on the node is reported as a touch until it is
 * released, wherever it goes. Hover, the wheel and the other buttons keep the mouse's own type.
 * Only the web can relabel pointers; elsewhere this does nothing.
 */
internal fun Modifier.mouseAsTouch(): Modifier = this then MouseAsTouchElement

private data object MouseAsTouchElement : ModifierNodeElement<MouseAsTouchNode>() {
    override fun create(): MouseAsTouchNode = MouseAsTouchNode()

    override fun update(node: MouseAsTouchNode) = Unit
}

private class MouseAsTouchNode : Modifier.Node(), GlobalPositionAwareModifierNode {
    private var bounds = Rect.Zero
    private var stop: (() -> Unit)? = null

    override fun onAttach() {
        stop = relabelMouseAsTouch { x, y -> bounds.contains(Offset(x, y)) }
    }

    override fun onDetach() {
        stop?.invoke()
        stop = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        bounds = coordinates.boundsInWindow()
    }
}

/**
 * Starts reporting the host's mouse presses as touches when [startsOnNode] accepts the window
 * position, in pixels, where they start. Returns a function that stops it, or null where the
 * platform's pointers can't be relabelled.
 */
internal expect fun relabelMouseAsTouch(startsOnNode: (x: Float, y: Float) -> Boolean): (() -> Unit)?
