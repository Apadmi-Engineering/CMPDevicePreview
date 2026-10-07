package com.kappmaker.devicepreview.core

import androidx.compose.ui.geometry.Size

/**
 * A device described by its logical screen size and pixel ratio, its safe areas, and optionally
 * the [frame] it is drawn in.
 *
 * The built-in presets ([com.kappmaker.devicepreview.devices.DevicePresets]) are generated from
 * the same device specs as flutter_device_preview's catalog.
 *
 * @property id Stable identifier, e.g. `apple-iphone-16-pro`.
 * @property portraitSize The logical screen size in portrait, in dp. Desktop presets use their
 * natural window size here (wider than tall).
 * @property devicePixelRatio Physical pixels per logical pixel.
 * @property brand The manufacturer, e.g. `Apple`, or null when unspecified.
 * @property year The release year, or null when unspecified.
 */
public data class DevicePreset(
    val id: String,
    override val name: String,
    override val type: DeviceType,
    val portraitSize: Size,
    val devicePixelRatio: Float,
    val kind: DeviceKind = DeviceKind.PHONE,
    val brand: String? = null,
    val year: Int? = null,
    override val portraitSafeArea: DeviceInsets = DeviceInsets.Zero,
    override val landscapeSafeArea: DeviceInsets = DeviceInsets.rotateToLandscape(portraitSafeArea),
    override val frame: DeviceFrame? = null,
) : Device {

    override val screenSize: ScreenSize
        get() = ScreenSize(
            width = portraitSize.width * devicePixelRatio,
            height = portraitSize.height * devicePixelRatio,
            density = devicePixelRatio,
        )

    override fun toString(): String = "DevicePreset($id, $name)"
}
