package com.kappmaker.devicepreview.core

import androidx.compose.ui.geometry.Size

/** The logical screen size in portrait. */
internal val Device.portraitScreenSize: Size
    get() = screenSize.let { Size(it.width / it.density, it.height / it.density) }

/**
 * Whether the device can be turned between portrait and landscape. A desktop window can't: it is
 * resized rather than rotated, so it stays in its natural, wide shape.
 */
internal val Device.isRotatable: Boolean
    get() = (this as? DevicePreset)?.kind != DeviceKind.DESKTOP

/**
 * Whether the device is laid out in portrait, the orientation its sizes and frame are described
 * in, when [isPortrait] is asked for. A device that can't rotate always is: a desktop preset's
 * portrait size is its natural window size, which is wider than tall.
 */
internal fun Device.laidOutInPortrait(isPortrait: Boolean): Boolean = isPortrait || !isRotatable

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

/**
 * Whether the device has a touchscreen, so the host's mouse acts as a finger on it. A desktop
 * window doesn't; a device of unknown kind does when it runs Android or iOS.
 */
internal val Device.isTouchscreen: Boolean
    get() = when (this) {
        is DevicePreset -> kind != DeviceKind.DESKTOP
        else -> type == DeviceType.ANDROID || type == DeviceType.IOS
    }
