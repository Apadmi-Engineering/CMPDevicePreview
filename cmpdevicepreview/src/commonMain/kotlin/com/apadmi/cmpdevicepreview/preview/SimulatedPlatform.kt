package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import com.apadmi.cmpdevicepreview.core.DeviceInsets

/**
 * Makes `WindowInsets` report [safeArea] and [keyboardHeight], in the content's dp, and
 * `isSystemInDarkTheme()` report [isDarkMode], inside [content]. Does nothing on Android, which has
 * no hooks for either.
 */
@Composable
internal expect fun SimulatedPlatform(
    safeArea: DeviceInsets,
    keyboardHeight: Float,
    isDarkMode: Boolean,
    content: @Composable () -> Unit,
)
