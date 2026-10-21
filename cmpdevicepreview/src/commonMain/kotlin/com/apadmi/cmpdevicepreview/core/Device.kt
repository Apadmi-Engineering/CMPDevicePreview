package com.apadmi.cmpdevicepreview.core

import androidx.compose.ui.geometry.Size

/** A device to show content in. Sizes, insets and keyboard heights are in logical pixels (dp). */
public interface Device {
    public val name: String
    public val type: DeviceType

    /** The screen size in portrait. */
    public val portraitSize: Size

    /** Physical pixels per logical pixel, or null when screenshots should use the host's. */
    public val devicePixelRatio: Float?

    /** Desktop devices can't be turned and don't take touch input; every other kind can and does. */
    public val kind: DeviceKind
        get() = DeviceKind.Phone

    /** The screen outline and body artwork, or null for a plain rectangular window. */
    public val frame: DeviceFrame?
        get() = null

    public val portraitSafeArea: DeviceInsets
        get() = DeviceInsets.Zero

    public val landscapeSafeArea: DeviceInsets
        get() = DeviceInsets.rotateToLandscape(portraitSafeArea)

    /** The status bar and gesture pill artwork, or null to leave the screen bare. */
    public val systemUi: DeviceSystemUi?
        get() = null

    /** The height the software keyboard covers in portrait, or null when there is none. */
    public val portraitKeyboardHeight: Float?
        get() = null

    /** The height the software keyboard covers in landscape, or null when there is none. */
    public val landscapeKeyboardHeight: Float?
        get() = null
}

/** The broad category of a simulated device. */
public enum class DeviceKind {
    Phone,
    Tablet,
    Foldable,
    Desktop,
    ;
}

/**
 * The platform a device runs, in the order the device picker lists them.
 *
 * @property displayName The platform's name as the device picker shows it.
 */
public enum class DeviceType(public val displayName: String) {
    Android("Android"),
    Ios("iOS"),
    Other("Other"),
    ;

    override fun toString(): String = displayName
}
