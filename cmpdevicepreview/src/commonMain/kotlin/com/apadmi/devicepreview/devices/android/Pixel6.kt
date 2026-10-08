package com.apadmi.devicepreview.devices.android

import com.apadmi.devicepreview.core.Device
import com.apadmi.devicepreview.core.DeviceType
import com.apadmi.devicepreview.core.ScreenSize

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