package com.apadmi.cmpdevicepreview.core

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * The shape a device's screen is cut to, and the body artwork drawn behind it.
 *
 * Coordinates are portrait dp with the origin at the screen's top-left corner, so the body starts
 * at `-screenOffset`. In landscape the whole frame is turned a quarter turn.
 *
 * @property size The size of the body artwork.
 * @property screenOffset Where the screen's top-left corner is in the body.
 * @property screenPath SVG path data (a `d` attribute) for the screen outline, cutouts included,
 * filled with the non-zero rule. The content is clipped to it. Empty for a rectangular screen.
 * @property body An SVG document drawn behind the screen, whose view box covers [size]. Only filled
 * `path`, `rect`, `circle`, `ellipse` and `polygon` shapes, groups, transforms and clip paths are
 * drawn. Empty for no body.
 */
@Immutable
public data class DeviceFrame(
    val size: Size,
    val screenOffset: Offset = Offset.Zero,
    val screenPath: String = "",
    val body: String = "",
) {

    /** Whether this frame carries no artwork at all. */
    public val isEmpty: Boolean
        get() = screenPath.isEmpty() && body.isEmpty()

    /**
     * The body's bounds in screen coordinates, for a screen of [screenSize] in the given
     * orientation. Landscape maps a portrait point `(x, y)` to `(y, portraitWidth − x)`.
     */
    public fun bodyBounds(screenSize: Size, isPortrait: Boolean): Rect {
        val portrait = Rect(Offset(-screenOffset.x, -screenOffset.y), size)
        if (isPortrait) return portrait
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
