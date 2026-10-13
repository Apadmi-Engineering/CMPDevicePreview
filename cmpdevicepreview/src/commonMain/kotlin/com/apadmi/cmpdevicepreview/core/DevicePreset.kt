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
 * @property name The display name, e.g. `iPhone 16 Pro`.
 * @property type The platform the device runs.
 * @property devicePixelRatio Physical pixels per logical pixel, which screenshots are taken at.
 * @property kind The broad category of device: a phone unless given.
 * @property portraitSafeArea The safe area in portrait, in dp, e.g. around a notch or a home
 * indicator. None unless given.
 * @property landscapeSafeArea The safe area in landscape, in dp. Unless given, it is derived from
 * [portraitSafeArea] by [DeviceInsets.rotateToLandscape].
 * @property frame The screen outline and body artwork, or null for a plain rectangular window.
 * @property systemUi The status bar and gesture pill artwork, or null to leave the screen bare.
 * @property portraitKeyboardHeight The height in dp the software keyboard covers in portrait, or
 * null when the device has none.
 * @property landscapeKeyboardHeight The height in dp the software keyboard covers in landscape, or
 * null when the device has none. It is not derived from the portrait one.
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
