package com.kappmaker.devicepreview.core

import androidx.compose.runtime.Immutable

/**
 * Artwork for the platform's own on-screen furniture — the status bar at the top, the gesture pill
 * or navigation bar at the bottom — drawn **on top of** the content, inside the simulated screen.
 *
 * Everything here is decoration: the clock is a static drawing that never ticks, the battery never
 * drains. It exists so a preview looks like the device it simulates.
 *
 * The bars are not positioned by this description: each one fills the device's safe area on its
 * side, which is already resolved per orientation. A bar whose safe area is zero — an iPhone status
 * bar in landscape, the gesture pill on a home-button device — is not drawn at all.
 *
 * Shapes that declare `fill="currentColor"` (or no fill) are tinted to contrast with the simulated
 * theme: white icons in dark mode, near-black ones in light mode.
 *
 * @property statusBar The status bar, drawn in the top safe area.
 * @property navigationBar The navigation bar or gesture pill, drawn in the bottom safe area.
 */
@Immutable
public data class DeviceSystemUi(
    val statusBar: SystemUiBar? = null,
    val navigationBar: SystemUiBar? = null,
) {

    /** Whether nothing would be drawn. */
    val isEmpty: Boolean
        get() = (statusBar?.isEmpty ?: true) && (navigationBar?.isEmpty ?: true)
}

/**
 * One system bar: up to three pieces of artwork laid out inside the safe area on its side of the
 * screen.
 *
 * Each piece is an SVG document drawn at its natural size — the size of its view box — never
 * stretched, so the same description works at any screen width:
 *
 * ```
 *  ┌──────────────────────────────────────────────────┐
 *  │ ⟨inset⟩ leading      center      trailing ⟨inset⟩ │  ← the safe area
 *  ├──────────────────────────────────────────────────┤
 *  │                     content                      │
 * ```
 *
 * Leading and trailing swap under a right-to-left layout direction.
 *
 * @property leading Artwork anchored to the leading edge — the clock, typically.
 * @property center Artwork centered horizontally — the gesture pill, typically.
 * @property trailing Artwork anchored to the trailing edge — the status icons, typically.
 * @property inset The distance from the screen edge to [leading] and [trailing].
 * @property bottomInset When set, the distance from the bar's outer edge to the bottom of the
 * artwork (how a home indicator sits); otherwise the artwork is centered vertically.
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
    val isEmpty: Boolean
        get() = leading.isEmpty() && center.isEmpty() && trailing.isEmpty()

    override fun toString(): String =
        "SystemUiBar(leading=${leading.length} chars, center=${center.length} chars, " +
            "trailing=${trailing.length} chars, inset=$inset, bottomInset=$bottomInset)"
}
