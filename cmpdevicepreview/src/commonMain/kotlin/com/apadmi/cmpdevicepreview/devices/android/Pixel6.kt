package com.apadmi.cmpdevicepreview.devices.android

import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceType
import com.apadmi.cmpdevicepreview.core.ScreenSize

public class Pixel6 : Device {
    override val name: String
        get() = "Pixel 6"


    override val screenSize: ScreenSize
        get() = ScreenSize(
            width = 1080f,
            height = 2400f,
            density = 420 / 160f
        )

    override val type: DeviceType
        get() = DeviceType.ANDROID

}