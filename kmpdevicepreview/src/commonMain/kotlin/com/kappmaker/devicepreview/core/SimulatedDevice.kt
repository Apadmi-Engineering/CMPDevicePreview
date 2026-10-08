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

/**
 * Keeps a raised keyboard raised across a device or orientation change, at the new height, unless
 * the device has no keyboard there.
 */
internal fun SimulatedDevice.droppingUnavailableKeyboard(): SimulatedDevice =
    if (configuration.isKeyboardVisible && currentKeyboardHeight == null) {
        copy(configuration = configuration.copy(isKeyboardVisible = false))
    } else {
        this
    }
