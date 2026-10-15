package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import com.apadmi.cmpdevicepreview.core.DeviceInsets

/**
 * Android's insets and theme come from the real window and configuration, which can't be
 * overridden, and its dialogs and popups are windows of their own.
 */
@Composable
internal actual fun SimulatedPlatform(
    safeArea: DeviceInsets,
    keyboardHeight: Float,
    isDarkMode: Boolean,
    screenShape: Shape,
    content: @Composable () -> Unit,
) {
    content()
}
