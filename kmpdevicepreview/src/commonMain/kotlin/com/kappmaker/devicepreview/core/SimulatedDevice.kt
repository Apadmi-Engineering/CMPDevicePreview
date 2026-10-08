package com.kappmaker.devicepreview.core

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
