package com.apadmi.devicepreview.devices

import com.apadmi.devicepreview.core.DevicePreset

/** The devices offered by the configuration view: by platform, then brand, newest first. */
internal val allDevices: List<DevicePreset> = DevicePresets.all.sortedWith(
    compareBy<DevicePreset> { it.type.order }
        .thenBy { it.brand.orEmpty() }
        .thenByDescending { it.year ?: 0 }
        .thenBy { it.name }
)
