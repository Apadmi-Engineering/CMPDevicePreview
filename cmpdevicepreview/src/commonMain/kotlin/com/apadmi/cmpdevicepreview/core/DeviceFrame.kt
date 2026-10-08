package com.apadmi.cmpdevicepreview.core

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * The physical appearance of a simulated device: the shape its screen is cut to, and the artwork
 * of the body drawn behind that screen.
 *
 * Everything is expressed in **portrait** logical pixels, in the coordinate space of the screen:
 * the screen's top-left corner is the origin, so the body starts at `-screenOffset`. Landscape is
 * not a separate description — the whole frame is rotated a quarter turn when drawn, like turning
 * the real device around.
 *
 * ```
 *        ┌───────────────────┐  ← body, at (-screenOffset)
 *        │  ┌─────────────┐  │
 *        │  │   screen    │  │  ← origin (0, 0) at the screen's top-left
 *        │  │             │  │
 *        │  └─────────────┘  │
 *        └───────────────────┘
 * ```
 *
 * Both artwork fields are optional: a frame with only a [screenPath] rounds the content's corners
 * with no body, and a frame with only a [body] draws the device around a rectangular screen.
 *
 * @property size The size of the body artwork.
 * @property screenOffset The offset of the screen's top-left corner inside the body.
 * @property screenPath SVG path data (a `d` attribute) for the screen outline, in screen
 * coordinates. Cutouts (notch, punch-hole, Dynamic Island) are part of it: the content is clipped
 * to this outline with the non-zero fill rule. Empty for a plain rectangular screen.
 * @property body An SVG document drawn behind the screen, whose view box covers [size]. Only a
 * subset of SVG is supported: filled `path`, `rect`, `circle`, `ellipse` and `polygon` shapes,
 * groups, transforms and clip paths. Empty when the frame only rounds the screen corners.
 */
@Immutable
public data class DeviceFrame(
    val size: Size,
    val screenOffset: Offset = Offset.Zero,
    val screenPath: String = "",
    val body: String = "",
) {

    /** Whether this frame carries no artwork at all. */
    val isEmpty: Boolean
        get() = screenPath.isEmpty() && body.isEmpty()

    /**
     * The body rectangle in screen coordinates for a screen of [screenSize] (already
     * orientation-resolved).
     *
     * In landscape the frame is rotated a quarter turn, mapping a portrait point `(x, y)` to
     * `(y, portraitWidth − x)`.
     */
    public fun bodyBounds(screenSize: Size, isPortrait: Boolean): Rect {
        val portrait = Rect(Offset(-screenOffset.x, -screenOffset.y), size)
        if (isPortrait) return portrait
        // In landscape the screen is (portraitHeight, portraitWidth).
        val portraitWidth = screenSize.height
        return Rect(
            left = portrait.top,
            top = portraitWidth - portrait.right,
            right = portrait.bottom,
            bottom = portraitWidth - portrait.left,
        )
    }

    override fun toString(): String =
        "DeviceFrame(size=$size, screenOffset=$screenOffset, " +
            "screenPath=${screenPath.length} chars, body=${body.length} chars)"
}
