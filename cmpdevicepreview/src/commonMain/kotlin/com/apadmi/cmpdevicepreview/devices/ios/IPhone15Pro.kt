package com.apadmi.cmpdevicepreview.devices.ios


import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceType
import com.apadmi.cmpdevicepreview.core.ScreenSize

public class IPhone15Pro : Device {
    override val name: String
        get() = "iPhone 15 Pro"

    override val screenSize: ScreenSize
        get() = ScreenSize(
            width = 1179f, // Logical pixels width
            height = 2556f, // Logical pixels height
            density = 3f // Device Pixel Ratio (DPR), 3x scale for iOS
        )

    override val type: DeviceType
        get() = DeviceType.IOS
}
