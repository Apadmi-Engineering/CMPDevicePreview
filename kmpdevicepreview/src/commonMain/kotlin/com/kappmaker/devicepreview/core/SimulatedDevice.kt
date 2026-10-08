package com.kappmaker.devicepreview.core

import androidx.compose.ui.geometry.Size

public data class SimulatedDevice(
    val device: Device,
    val configuration: DeviceConfiguration = DeviceConfiguration()
){

    /** Whether the device is shown in portrait: always, for a device that can't rotate. */
    private val isPortrait: Boolean
        get() = device.laidOutInPortrait(configuration.isPortrait)

    val currentScreenSize: ScreenSize
        get() = if (isPortrait) {
            device.screenSize
        } else {
            device.screenSize.rotated()
        }

    /** The device's safe area in the current orientation, not including a raised keyboard. */
    val currentSafeArea: DeviceInsets
        get() = device.safeAreaIn(isPortrait)

    /**
     * The height of the device's keyboard in the current orientation, or null when it has none
     * there. Whether it is raised is [DeviceConfiguration.isKeyboardVisible].
     */
    val currentKeyboardHeight: Float?
        get() = device.keyboardHeightIn(isPortrait)
}

/** The size of the device's screen in its current orientation, in logical pixels. */
internal val SimulatedDevice.currentLogicalSize: Size
    get() = currentScreenSize.let { Size(it.width / it.density, it.height / it.density) }

/** The size of the window the content sees, in its dp: smaller when the display size is larger. */
internal val SimulatedDevice.currentContentSize: Size
    get() = currentLogicalSize / configuration.displayScale

/**
 * Fits the configuration to a changed device or orientation. A raised keyboard stays raised, at the
 * new height, unless the device has no keyboard there. The display size moves to the nearest one
 * the device offers. The text size is kept: every device offers the same ones.
 */
internal fun SimulatedDevice.adaptedToDevice(): SimulatedDevice {
    val adapted = configuration.copy(
        isKeyboardVisible = configuration.isKeyboardVisible && currentKeyboardHeight != null,
        displayScale = device.displayScaleSteps.nearest(configuration.displayScale).scale,
    )
    return if (adapted == configuration) this else copy(configuration = adapted)
}
