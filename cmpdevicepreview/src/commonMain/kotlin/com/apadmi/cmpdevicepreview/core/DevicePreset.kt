package com.apadmi.cmpdevicepreview.core

import androidx.compose.ui.geometry.Size

/**
 * A [Device] described by plain values. The built-in presets in
 * [com.apadmi.cmpdevicepreview.devices.DevicePresets] are generated from the same device specs as
 * flutter_device_preview's catalog.
 *
 * @property id Stable identifier, e.g. `apple-iphone-16-pro`.
 * @property portraitSize The screen size in portrait. Desktop presets use their natural window
 * size, which is wider than tall.
 * @property brand The manufacturer, e.g. `Apple`, or null when unspecified.
 * @property year The release year, or null when unspecified.
 */
public data class DevicePreset(
    val id: String,
    override val name: String,
    override val type: DeviceType,
    override val portraitSize: Size,
    override val devicePixelRatio: Float,
    override val kind: DeviceKind = DeviceKind.PHONE,
    val brand: String? = null,
    val year: Int? = null,
    override val portraitSafeArea: DeviceInsets = DeviceInsets.Zero,
    override val landscapeSafeArea: DeviceInsets = DeviceInsets.rotateToLandscape(portraitSafeArea),
    override val frame: DeviceFrame? = null,
    override val systemUi: DeviceSystemUi? = null,
    override val portraitKeyboardHeight: Float? = null,
    override val landscapeKeyboardHeight: Float? = null,
) : Device {

    override fun toString(): String = "DevicePreset($id, $name)"
}
