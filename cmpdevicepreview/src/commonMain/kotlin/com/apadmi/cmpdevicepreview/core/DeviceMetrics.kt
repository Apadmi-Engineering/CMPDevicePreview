package com.apadmi.cmpdevicepreview.core

import androidx.compose.ui.geometry.Size

/** Whether the device can be turned between portrait and landscape. A desktop window is resized instead. */
internal val Device.isRotatable: Boolean
    get() = kind != DeviceKind.Desktop

/** Whether the device has frame artwork; one without is drawn as a plain window. */
internal val Device.hasFrameArtwork: Boolean
    get() = frame?.isEmpty == false

/** Whether the host's mouse acts as a finger on the device. */
internal val Device.isTouchscreen: Boolean
    get() = kind != DeviceKind.Desktop

/**
 * Whether the device is laid out in portrait when [orientation] is asked for. A device that can't
 * rotate always is, since its portrait size is its natural one.
 *
 * @param orientation The orientation the device is asked to be shown in.
 * @return Whether the device is laid out in portrait.
 */
internal fun Device.isLaidOutInPortrait(orientation: DeviceOrientation): Boolean =
    orientation == DeviceOrientation.Portrait || !isRotatable

/** This size a quarter turn round: its width and height swapped.
 *
 * @return*/
internal fun Size.turned(): Size = Size(height, width)

internal fun Device.screenSizeIn(isPortrait: Boolean): Size =
    if (isPortrait) portraitSize else portraitSize.turned()

internal fun Device.safeAreaIn(isPortrait: Boolean): DeviceInsets =
    if (isPortrait) portraitSafeArea else landscapeSafeArea

/**
 * The keyboard height in the given orientation; the landscape one is not derived from portrait.
 *
 * @param isPortrait Whether to take the portrait keyboard height rather than the landscape one.
 * @return The keyboard height in dp, or null when the device has no keyboard in that orientation.
 */
internal fun Device.keyboardHeightIn(isPortrait: Boolean): Float? =
    if (isPortrait) portraitKeyboardHeight else landscapeKeyboardHeight
