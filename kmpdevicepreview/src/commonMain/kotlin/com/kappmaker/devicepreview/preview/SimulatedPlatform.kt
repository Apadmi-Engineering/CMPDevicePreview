package com.kappmaker.devicepreview.preview

import androidx.compose.runtime.Composable
import com.kappmaker.devicepreview.core.DeviceInsets

/**
 * Makes the platform's own APIs inside [content] describe the simulated device, so the content
 * needs nothing from this library: `WindowInsets` (and the padding modifiers built on it) report
 * [safeArea] and the keyboard's [keyboardHeight], and `isSystemInDarkTheme()` reports [isDarkMode].
 *
 * The insets are in the device's logical pixels. Android has no such hooks: there the content sees
 * the real window.
 */
@Composable
internal expect fun SimulatedPlatform(
    safeArea: DeviceInsets,
    keyboardHeight: Float,
    isDarkMode: Boolean,
    content: @Composable () -> Unit,
)
