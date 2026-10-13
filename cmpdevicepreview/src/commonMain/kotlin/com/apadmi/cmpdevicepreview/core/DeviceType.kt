package com.apadmi.cmpdevicepreview.core

/** The platform a device runs, in the order the device picker lists them. */
public enum class DeviceType(public val displayName: String) {
    ANDROID("Android"),
    IOS("iOS"),
    OTHER("Other");

    override fun toString(): String = displayName
}
