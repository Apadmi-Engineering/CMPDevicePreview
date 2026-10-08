package com.apadmi.devicepreview.core

import androidx.compose.ui.unit.LayoutDirection

/**
 * How a simulated device is shown.
 *
 * @property isSystemUiVisible Whether the status bar and gesture pill are drawn. Hiding them
 * changes nothing the content can observe: the safe area stays the same.
 * @property isKeyboardVisible Whether the device's software keyboard is raised, which draws its
 * band and adds it to the insets, whatever [isSystemUiVisible] is. Has no effect on a device
 * without a keyboard height for the current orientation.
 * @property fontScale How much larger text is than at the default text size: what
 * `LocalDensity.current.fontScale` reports inside the device.
 * @property displayScale How much larger everything on the screen is drawn, as Android's Display
 * size setting does: the content's window is the screen's size divided by this, in dp.
 * @property layoutDirection The layout direction inside the device, which mirrors its status bar
 * too.
 * @property backdrop The colour behind the device.
 * @property isDotGridVisible Whether the backdrop has a grid of dots.
 */
public data class DeviceConfiguration(
    val isPortrait: Boolean = true,
    val isDarkMode: Boolean = false,
    val isPreviewModeEnabled: Boolean = true,
    val isFrameVisible: Boolean = true,
    val isSystemUiVisible: Boolean = true,
    val isKeyboardVisible: Boolean = false,
    val fontScale: Float = 1f,
    val displayScale: Float = 1f,
    val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    val backdrop: Backdrop = Backdrop.LIGHT,
    val isDotGridVisible: Boolean = true,
)
