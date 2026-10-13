package com.apadmi.cmpdevicepreview.core

import androidx.compose.ui.geometry.Size

/** The size of the window the content sees, in its dp. */
internal val SimulatedDevice.currentContentSize: Size
    get() = currentSize / configuration.displayScale

/**
 * A [device] and how it is shown.
 *
 * @property device The device being shown.
 * @property configuration How the device is shown, such as its orientation, theme and display size.
 */
public data class SimulatedDevice(
    val device: Device,
    val configuration: DeviceConfiguration = DeviceConfiguration(),
) {
    /** Whether the device is shown in portrait: always, for a device that can't rotate. */
    private val isPortrait: Boolean
        get() = device.isLaidOutInPortrait(configuration.isPortrait)

    /** The screen size in the current orientation, in dp. */
    val currentSize: Size
        get() = device.screenSizeIn(isPortrait)

    /** The safe area in the current orientation, not including a raised keyboard. */
    val currentSafeArea: DeviceInsets
        get() = device.safeAreaIn(isPortrait)

    /** The keyboard height in the current orientation, or null when the device has none there. */
    val currentKeyboardHeight: Float?
        get() = device.keyboardHeightIn(isPortrait)

    /**
     * Fits the configuration to a changed device or orientation. A raised keyboard stays raised unless
     * the device has none in this orientation, and the display size moves to the nearest one the
     * device offers. Every device offers the same text sizes, so the text size is kept.
     *
     * @return This device, or a copy with the configuration fitted to it.
     */
    internal fun adaptedToDevice(): SimulatedDevice {
        val adapted = configuration.copy(
            isKeyboardVisible = configuration.isKeyboardVisible && currentKeyboardHeight != null,
            displayScale = device.displayScaleSteps.nearest(configuration.displayScale).scale,
        )
        return if (adapted == configuration) this else copy(configuration = adapted)
    }
}
