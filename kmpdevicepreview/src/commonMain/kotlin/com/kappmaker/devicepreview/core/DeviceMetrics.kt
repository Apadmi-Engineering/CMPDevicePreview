package com.kappmaker.devicepreview.core

import androidx.compose.ui.geometry.Size

/** The logical screen size in portrait. */
internal val Device.portraitScreenSize: Size
    get() = screenSize.let { Size(it.width / it.density, it.height / it.density) }

/** The safe area in the given orientation. */
internal fun Device.safeAreaIn(isPortrait: Boolean): DeviceInsets =
    if (isPortrait) portraitSafeArea else landscapeSafeArea

/**
 * The keyboard height in the given orientation, or null when the device has no keyboard there.
 *
 * There is no rotation rule between the two heights: a landscape keyboard is not a rotated
 * portrait one.
 */
internal fun Device.keyboardHeightIn(isPortrait: Boolean): Float? =
    if (isPortrait) portraitKeyboardHeight else landscapeKeyboardHeight
