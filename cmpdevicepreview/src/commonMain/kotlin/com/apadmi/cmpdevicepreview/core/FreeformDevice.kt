package com.apadmi.cmpdevicepreview.core

import androidx.compose.ui.geometry.Size

/**
 * A plain window of any size: a bordered rectangle with no insets, status bar, keyboard or touch
 * input, resized by dragging its corners in the preview. It can't be turned.
 *
 * @property size The window's size, in logical pixels (dp), like [DevicePreset.portraitSize].
 */
public data class FreeformDevice(val size: Size) : Device {

    override val name: String
        get() = "Freeform"

    override val screenSize: ScreenSize
        get() = ScreenSize(width = size.width, height = size.height, density = 1f)

    override val type: DeviceType
        get() = DeviceType.OTHER
}
