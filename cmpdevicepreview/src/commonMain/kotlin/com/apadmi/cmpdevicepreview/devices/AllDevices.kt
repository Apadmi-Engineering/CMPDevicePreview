package com.apadmi.cmpdevicepreview.devices

import com.apadmi.cmpdevicepreview.core.DevicePreset

/** The devices offered by the configuration view: by platform, then brand, newest first. */
internal val allDevices: List<DevicePreset> = DevicePresets.all.sortedWith(
    compareBy<DevicePreset> { it.type }
        .thenBy { it.brand.orEmpty() }
        .thenByDescending { it.year ?: 0 }
        .thenBy { it.name }
)
