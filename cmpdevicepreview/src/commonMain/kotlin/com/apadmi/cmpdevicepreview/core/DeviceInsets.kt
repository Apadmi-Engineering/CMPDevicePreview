package com.apadmi.cmpdevicepreview.core

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp

/**
 * Insets from each edge of a simulated screen, in logical pixels (the dp the previewed content
 * sees), e.g. the safe area around a notch, a Dynamic Island or a home indicator.
 */
public data class DeviceInsets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
) {

    /** These insets as [WindowInsets], for `Modifier.windowInsetsPadding` or a `Scaffold`. */
    public fun toWindowInsets(): WindowInsets =
        WindowInsets(left = left.dp, top = top.dp, right = right.dp, bottom = bottom.dp)

    public companion object {
        public val Zero: DeviceInsets = DeviceInsets()

        /**
         * Derives landscape insets from portrait ones: the notch/status area is mirrored to both
         * sides and the home indicator is kept at the bottom.
         */
        public fun rotateToLandscape(portrait: DeviceInsets): DeviceInsets =
            DeviceInsets(left = portrait.top, right = portrait.top, bottom = portrait.bottom)
    }
}

/** These insets, each divided by [scale]. */
internal operator fun DeviceInsets.div(scale: Float): DeviceInsets =
    DeviceInsets(left = left / scale, top = top / scale, right = right / scale, bottom = bottom / scale)
