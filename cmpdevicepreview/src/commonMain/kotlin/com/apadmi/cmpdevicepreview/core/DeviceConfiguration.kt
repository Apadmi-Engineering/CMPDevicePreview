package com.apadmi.cmpdevicepreview.core

import androidx.compose.ui.unit.LayoutDirection

/**
 * How a simulated device is shown.
 *
 * @property orientation Which way up the device is held. Devices that can't turn, such as desktop
 * windows, ignore it.
 * @property appearance Whether `isSystemInDarkTheme()` returns true inside the device, which is
 * when this is [Appearance.Dark]. It also sets the colour of the status bar's icons.
 * @property isFrameVisible Whether the device's body is drawn. The content is clipped to the
 * screen outline either way.
 * @property isSystemUiVisible Whether the status bar and gesture pill are drawn. The safe area is
 * the same either way.
 * @property isKeyboardVisible Whether the software keyboard is raised, which draws its band and
 * adds it to the insets. Devices without a keyboard in the current orientation ignore it.
 * @property fontScale What `LocalDensity.current.fontScale` reports inside the device. Text grows
 * in proportion to it at every size.
 * @property displayScale How much larger the content is drawn, like Android's Display size
 * setting. The window and insets the content sees are this much smaller in dp; the frame, status
 * bar and keyboard band don't change.
 * @property layoutDirection The layout direction inside the device, which also mirrors its status
 * bar, or null for the host's.
 * @property language The BCP 47 tag of the language Compose Resources reads inside the device, such
 * as `cy`, or null for the host's. Resources of the platform itself, such as Android's `R.string`,
 * don't follow it.
 */
public data class DeviceConfiguration(
    val orientation: DeviceOrientation = DeviceOrientation.Portrait,
    val appearance: Appearance = Appearance.Light,
    val isFrameVisible: Boolean = true,
    val isSystemUiVisible: Boolean = true,
    val isKeyboardVisible: Boolean = false,
    val fontScale: Float = 1f,
    val displayScale: Float = 1f,
    val layoutDirection: LayoutDirection? = null,
    val language: String? = null,
)

/** Which way up a simulated device is held. */
public enum class DeviceOrientation {
    Portrait,
    Landscape,
    ;
}

/** Whether a simulated device is in light or dark mode. */
public enum class Appearance {
    Light,
    Dark,
    ;
}

/**
 * How the preview around a simulated device is shown.
 *
 * @property backdrop The colour behind the device.
 * @property isDotGridVisible Whether the backdrop has a grid of dots.
 * @property isPreviewModeEnabled Whether `DeviceWithConfigurationView` shows the device and its
 * panel. When false it shows just the content, with a button to turn it back on.
 */
public data class PreviewSettings(
    val backdrop: Backdrop = Backdrop.Light,
    val isDotGridVisible: Boolean = true,
    val isPreviewModeEnabled: Boolean = true,
)

/** The colour behind a simulated device. */
public enum class Backdrop {
    Light,
    Dark,

    /** Light or dark, as the simulated device's appearance is. */
    Auto,
    ;
}
