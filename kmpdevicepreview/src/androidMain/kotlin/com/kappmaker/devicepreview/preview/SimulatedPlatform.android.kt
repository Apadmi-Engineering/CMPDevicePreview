package com.kappmaker.devicepreview.preview

import androidx.compose.runtime.Composable
import com.kappmaker.devicepreview.core.DeviceInsets

/** Android's insets and theme come from the real window and configuration, which can't be overridden. */
@Composable
internal actual fun SimulatedPlatform(
    safeArea: DeviceInsets,
    keyboardHeight: Float,
    isDarkMode: Boolean,
    content: @Composable () -> Unit,
) {
    content()
}
