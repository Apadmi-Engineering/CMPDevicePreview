package com.kappmaker.devicepreview.core

/**
 * How a simulated device is shown.
 *
 * @property isSystemUiVisible Whether the status bar and gesture pill are drawn. Hiding them
 * changes nothing the content can observe: the safe area stays the same.
 * @property isKeyboardVisible Whether the device's software keyboard is raised, which draws its
 * band and adds it to the insets, whatever [isSystemUiVisible] is. Has no effect on a device
 * without a keyboard height for the current orientation.
 */
public data class DeviceConfiguration(
    val isPortrait: Boolean = true,
    val isDarkMode: Boolean = false,
    val isPreviewModeEnabled: Boolean = true,
    val isFrameVisible: Boolean = true,
    val isSystemUiVisible: Boolean = true,
    val isKeyboardVisible: Boolean = false,
)
