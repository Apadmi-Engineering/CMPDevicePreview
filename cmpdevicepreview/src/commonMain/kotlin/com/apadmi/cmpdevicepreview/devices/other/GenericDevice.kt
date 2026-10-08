package com.apadmi.cmpdevicepreview.devices.other

import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceType
import com.apadmi.cmpdevicepreview.core.ScreenSize

public class GenericDevice : Device {

    override val name: String
        get() = "Generic Device"
    override val screenSize: ScreenSize
        get() = ScreenSize(
            width = 1080f,
            height = 2400f,
            density = 420 / 160f
        )

    override val type: DeviceType
        get() = DeviceType.OTHER
}
