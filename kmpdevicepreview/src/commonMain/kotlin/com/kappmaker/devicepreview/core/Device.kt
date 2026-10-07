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

    /** The device's status bar and gesture pill artwork, or null to leave the screen bare. */
    public val systemUi: DeviceSystemUi?
        get() = null

    /**
     * The height the device's software keyboard covers in portrait, in logical pixels, or null when
     * the device has no software keyboard.
     */
    public val portraitKeyboardHeight: Float?
        get() = null

    /**
     * The height the device's software keyboard covers in landscape, in logical pixels, or null
     * when the device has no software keyboard in landscape.
     */
    public val landscapeKeyboardHeight: Float?
        get() = null
}
