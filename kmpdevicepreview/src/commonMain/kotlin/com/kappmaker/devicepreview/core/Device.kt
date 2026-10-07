package com.kappmaker.devicepreview.core


public interface Device {
    public val name: String             // Device name (e.g., "Pixel 6")
    public val screenSize: ScreenSize   // Screen dimensions
    public val type: DeviceType

    /** The device's screen outline and body artwork, or null for a plain rectangular screen. */
    public val frame: DeviceFrame?
        get() = null

    /** The safe area in portrait, in logical pixels. */
    public val portraitSafeArea: DeviceInsets
        get() = DeviceInsets.Zero

    /** The safe area in landscape, in logical pixels. */
    public val landscapeSafeArea: DeviceInsets
        get() = DeviceInsets.rotateToLandscape(portraitSafeArea)
}
