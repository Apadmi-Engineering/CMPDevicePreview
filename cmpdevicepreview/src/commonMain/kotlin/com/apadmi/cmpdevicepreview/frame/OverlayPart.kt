package com.apadmi.cmpdevicepreview.frame

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.unit.LayoutDirection
import com.apadmi.cmpdevicepreview.core.DeviceInsets

/**
 * Something the preview draws over the device's content, such as the keyboard band or a bar.
 *
 * @property region The band it covers, in screen coordinates, which HTML elements are kept out of.
 * @property paint Paints it on a canvas whose origin is the screen's top-left corner, in screen units.
 */
internal class OverlayPart(val region: Rect, val paint: (Canvas) -> Unit)

// In paint order: the keyboard band first, so the gesture pill sits on top of it. Mirrored in RTL, as
// a phone's status bar is.
internal fun overlayParts(
    systemUi: SystemUiPainter?,
    keyboardInset: Float,
    screenSize: Size,
    safeArea: DeviceInsets,
    layoutDirection: LayoutDirection,
    isDarkMode: Boolean,
): List<OverlayPart> {
    val keyboard = keyboardBand(screenSize, keyboardInset)?.let { band ->
        OverlayPart(band) { canvas -> paintSimulatedKeyboard(canvas, screenSize, keyboardInset) }
    }
    val iconColor = SystemUiColors.icons(isDarkMode)
    val bars = systemUi?.bars(screenSize, safeArea, layoutDirection).orEmpty().map { bar ->
        OverlayPart(bar.region) { canvas -> paintSystemUi(canvas, bar.artwork, iconColor) }
    }
    return listOfNotNull(keyboard) + bars
}
