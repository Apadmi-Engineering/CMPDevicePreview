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
     *
     * @param screenSize The size of the screen the body is drawn around, in dp.
     * @param isPortrait Whether the screen is in portrait; otherwise the body is turned a quarter turn.
     * @return The body's bounds, in the screen's coordinates.
     */
    public fun bodyBounds(screenSize: Size, isPortrait: Boolean): Rect {
        val portrait = Rect(Offset(-screenOffset.x, -screenOffset.y), size)
        if (isPortrait) {
            return portrait
        }
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

/**
 * Static artwork for the status bar and the navigation bar or gesture pill, drawn over the content.
 *
 * Each bar fills the device's safe area on its side in the current orientation, and isn't drawn
 * where that safe area is zero, such as an iPhone's status bar in landscape. Shapes filled with
 * `currentColor`, or not filled at all, are tinted to contrast with the theme.
 *
 * @property statusBar Drawn in the top safe area.
 * @property navigationBar Drawn in the bottom safe area.
 */
@Immutable
public data class DeviceSystemUi(
    val statusBar: SystemUiBar? = null,
    val navigationBar: SystemUiBar? = null,
) {
    /** Whether nothing would be drawn. */
    @Suppress("WRONG_NAME_OF_VARIABLE_INSIDE_ACCESSOR")  // The accessor reads each bar's own isEmpty, not this one.
    public val isEmpty: Boolean
        get() = (statusBar?.isEmpty ?: true) && (navigationBar?.isEmpty ?: true)
}

/**
 * Up to three pieces of SVG artwork in one bar. Each is drawn at the size of its view box, never
 * stretched, so the same bar fits any screen width. Leading and trailing swap in a right-to-left
 * layout direction.
 *
 * @property leading Artwork at the leading edge, such as the clock.
 * @property center Artwork in the middle, such as the gesture pill.
 * @property trailing Artwork at the trailing edge, such as the status icons.
 * @property inset The distance from the screen edge to [leading] and [trailing].
 * @property bottomInset When set, the distance from the bar's outer edge to the bottom of the
 * artwork, as a home indicator sits. Otherwise the artwork is centred vertically.
 */
@Immutable
public data class SystemUiBar(
    val leading: String = "",
    val center: String = "",
    val trailing: String = "",
    val inset: Float = 16f,
    val bottomInset: Float? = null,
) {
    /** Whether this bar has no artwork. */
    public val isEmpty: Boolean
        get() = leading.isEmpty() && center.isEmpty() && trailing.isEmpty()

    override fun toString(): String =
        "SystemUiBar(leading=${leading.length} chars, center=${center.length} chars, " +
                "trailing=${trailing.length} chars, inset=$inset, bottomInset=$bottomInset)"
}
