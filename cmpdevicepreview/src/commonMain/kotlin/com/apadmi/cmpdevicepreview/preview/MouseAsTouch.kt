@file:Suppress("FILE_NAME_MATCH_CLASS")

// Named for the feature, like its platform files.

package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement

// Null where the platform can't relabel pointers, and there the toolbar has no drag-to-scroll switch.
internal expect val mouseAsTouchRelabeller: MouseAsTouchRelabeller?

// startsOnNode takes window pixels, and the function returned stops the relabelling.
internal typealias MouseAsTouchRelabeller = (startsOnNode: (x: Float, y: Float) -> Boolean) -> () -> Unit

private data object MouseAsTouchElement : ModifierNodeElement<MouseAsTouchNode>() {
    override fun create(): MouseAsTouchNode = MouseAsTouchNode()

    override fun update(node: MouseAsTouchNode) = Unit
}

private class MouseAsTouchNode : Modifier.Node(), GlobalPositionAwareModifierNode {
    private var bounds = Rect.Zero
    private var stop: (() -> Unit)? = null

    override fun onAttach() {
        stop = mouseAsTouchRelabeller?.invoke { x, y -> bounds.contains(Offset(x, y)) }
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
 * Makes the host's mouse act as a finger on this node, since Compose's scrollables ignore mouse
 * drags: a primary-button press that starts on the node is reported as a touch until it is
 * released. Hover, the wheel and other buttons stay mouse events. Only the web can relabel
 * pointers; elsewhere this does nothing.
 *
 * @return This modifier, with the node that relabels the mouse added.
 */
internal fun Modifier.mouseAsTouch(): Modifier = this then MouseAsTouchElement
