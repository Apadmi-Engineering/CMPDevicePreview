package com.apadmi.cmpdevicepreview.core

/**
 * How the preview around a simulated device is shown.
 *
 * @property backdrop The colour behind the device.
 * @property isDotGridVisible Whether the backdrop has a grid of dots.
 * @property isPreviewModeEnabled Whether `DeviceWithConfigurationView` shows the device and its
 * panel. When false it shows just the content, with a button to turn it back on.
 */
public data class PreviewSettings(
    val backdrop: Backdrop = Backdrop.LIGHT,
    val isDotGridVisible: Boolean = true,
    val isPreviewModeEnabled: Boolean = true,
)
