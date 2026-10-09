package com.apadmi.cmpdevicepreview.core

/** Whether the device can be turned between portrait and landscape. A desktop window is resized instead. */
internal val Device.isRotatable: Boolean
    get() = kind != DeviceKind.DESKTOP

/** Whether the device has frame artwork; one without is drawn as a plain window. */
internal val Device.hasFrameArtwork: Boolean
    get() = frame?.isEmpty == false

/** Whether the host's mouse acts as a finger on the device. */
internal val Device.isTouchscreen: Boolean
    get() = kind != DeviceKind.DESKTOP

/**
 * Whether the device is laid out in portrait when [isPortrait] is asked for. A device that can't
 * rotate always is, since its portrait size is its natural one.
 */
internal fun Device.laidOutInPortrait(isPortrait: Boolean): Boolean = isPortrait || !isRotatable

internal fun Device.safeAreaIn(isPortrait: Boolean): DeviceInsets =
    if (isPortrait) portraitSafeArea else landscapeSafeArea

/** The keyboard height in the given orientation; the landscape one is not derived from portrait. */
internal fun Device.keyboardHeightIn(isPortrait: Boolean): Float? =
    if (isPortrait) portraitKeyboardHeight else landscapeKeyboardHeight
