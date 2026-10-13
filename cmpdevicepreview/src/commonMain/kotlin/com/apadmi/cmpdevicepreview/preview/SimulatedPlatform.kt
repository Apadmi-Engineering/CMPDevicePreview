package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import com.apadmi.cmpdevicepreview.core.DeviceInsets

/**
 * Makes `WindowInsets` report [safeArea] and [keyboardHeight], in the content's dp, and
 * `isSystemInDarkTheme()` report [isDarkMode], inside [content]. Does nothing on Android, which has
 * no hooks for either.
 *
 * @param safeArea The insets `WindowInsets` reports for the status bar, the navigation bar and the
 * display cutout, in the content's dp.
 * @param keyboardHeight The height `WindowInsets` reports for the keyboard, in the content's dp.
 * @param isDarkMode Whether `isSystemInDarkTheme()` reports dark.
 * @param content What is shown with these insets and this theme.
 */
@Composable
internal expect fun SimulatedPlatform(
    safeArea: DeviceInsets,
    keyboardHeight: Float,
    isDarkMode: Boolean,
    content: @Composable () -> Unit,
)
