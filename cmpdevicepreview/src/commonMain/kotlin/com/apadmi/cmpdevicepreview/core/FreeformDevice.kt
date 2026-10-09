package com.apadmi.cmpdevicepreview.core

import androidx.compose.ui.geometry.Size

/**
 * A plain window of any size, resized by dragging its corners in the preview. It has no frame,
 * insets, status bar or keyboard, and like a desktop window it can't be turned.
 *
 * @property size The window's size, in dp.
 */
public data class FreeformDevice(val size: Size) : Device {

    override val name: String
        get() = "Freeform"

    override val type: DeviceType
        get() = DeviceType.OTHER

    override val portraitSize: Size
        get() = size

    override val devicePixelRatio: Float?
        get() = null

    override val kind: DeviceKind
        get() = DeviceKind.DESKTOP
}
