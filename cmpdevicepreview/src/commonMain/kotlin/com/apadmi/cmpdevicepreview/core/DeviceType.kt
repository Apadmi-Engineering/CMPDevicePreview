package com.apadmi.cmpdevicepreview.core

/**
 * The platform a device runs, in the order the device picker lists them.
 *
 * @property displayName The platform's name as the device picker shows it.
 */
public enum class DeviceType(public val displayName: String) {
    ANDROID("Android"),
    IOS("iOS"),
    OTHER("Other"),
    ;

    override fun toString(): String = displayName
}
