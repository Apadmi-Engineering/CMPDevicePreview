package com.kappmaker.devicepreview.core

public data class SimulatedDevice(
    val device: Device,
    val configuration: DeviceConfiguration = DeviceConfiguration()
){

    val currentScreenSize: ScreenSize
        get() = if (configuration.isPortrait) {
            device.screenSize
        } else {
            device.screenSize.rotated()
        }

    /** The device's safe area in the current orientation, not including a raised keyboard. */
    val currentSafeArea: DeviceInsets
        get() = device.safeAreaIn(configuration.isPortrait)

    /**
     * The height of the device's keyboard in the current orientation, or null when it has none
     * there. Whether it is raised is [DeviceConfiguration.isKeyboardVisible].
     */
    val currentKeyboardHeight: Float?
        get() = device.keyboardHeightIn(configuration.isPortrait)
}
