package com.apadmi.cmpdevicepreview.core

import androidx.compose.runtime.Immutable

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
