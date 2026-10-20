package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import com.apadmi.cmpdevicepreview.core.Appearance
import com.apadmi.cmpdevicepreview.core.Backdrop
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.DeviceOrientation
import com.apadmi.cmpdevicepreview.core.DevicePreset
import com.apadmi.cmpdevicepreview.core.FontScaleSteps
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.core.nearest
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.math.roundToInt

private const val KeyPrefix = "dp."
private const val DeviceKey = "device"
private const val SizeKey = "size"
private const val OrientationKey = "orientation"
private const val AppearanceKey = "appearance"
private const val FontScaleKey = "fontScale"
private const val DisplayScaleKey = "displayScale"
private const val DirectionKey = "direction"
private const val KeyboardKey = "keyboard"
private const val BezelKey = "bezel"
private const val SystemUiKey = "systemUi"
private const val BackdropKey = "backdrop"
private const val DotGridKey = "dotGrid"
private const val PreviewKey = "preview"

private const val Freeform = "freeform"
private const val Auto = "auto"
private const val On = "1"
private const val Off = "0"

private val PercentEscape = Regex("%([0-9a-fA-F]{2})")

private val TrueValues = setOf(On, "true", "on")
private val FalseValues = setOf(Off, "false", "off")

private val Enum<*>.linkValue: String
    get() = name.lowercase()

private fun Map<String, String>.configuration(initial: DeviceConfiguration) = DeviceConfiguration(
    orientation = enumValue<DeviceOrientation>(OrientationKey) ?: initial.orientation,
    appearance = enumValue<Appearance>(AppearanceKey) ?: initial.appearance,
    isFrameVisible = flag(BezelKey) ?: initial.isFrameVisible,
    isSystemUiVisible = flag(SystemUiKey) ?: initial.isSystemUiVisible,
    isKeyboardVisible = flag(KeyboardKey) ?: initial.isKeyboardVisible,
    fontScale = scale(FontScaleKey)?.let { FontScaleSteps.nearest(it).scale } ?: initial.fontScale,
    displayScale = scale(DisplayScaleKey) ?: initial.displayScale,
    layoutDirection = layoutDirection(initial.layoutDirection),
)

private fun Map<String, String>.previewSettings(initial: PreviewSettings) = PreviewSettings(
    backdrop = enumValue<Backdrop>(BackdropKey) ?: initial.backdrop,
    isDotGridVisible = flag(DotGridKey) ?: initial.isDotGridVisible,
    isPreviewModeEnabled = flag(PreviewKey) ?: initial.isPreviewModeEnabled,
)

private fun String.percentDecoded(): String = if ('%' in this) {
    PercentEscape.replace(this) { escape -> escape.groupValues[1].toInt(16).toChar().toString() }
} else {
    this
}

private fun MutableList<Pair<String, String>>.addFlag(
    key: String,
    value: Boolean,
    initial: Boolean
) {
    if (value != initial) {
        add(key to if (value) On else Off)
    }
}

private fun Map<String, String>.flag(key: String): Boolean? = when (this[key]?.lowercase()) {
    in TrueValues -> true
    in FalseValues -> false
    else -> null
}

private fun Map<String, String>.scale(key: String): Float? = this[key]?.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }

private inline fun <reified T : Enum<T>> Map<String, String>.enumValue(key: String): T? =
    enumValues<T>().firstOrNull { it.name.equals(this[key], ignoreCase = true) }

private fun Map<String, String>.layoutDirection(initial: LayoutDirection?): LayoutDirection? =
    if (this[DirectionKey].equals(Auto, ignoreCase = true)) null else enumValue<LayoutDirection>(DirectionKey) ?: initial

private fun Map<String, String>.device(): Device? {
    val id = this[DeviceKey]?.lowercase() ?: return null
    if (id != Freeform) {
        return DevicePresets.byId(id)
    }
    val sides = this[SizeKey]?.lowercase()?.split('x')?.map(String::toIntOrNull)
    val (width, height) = sides?.takeIf { it.size == 2 } ?: return null
    return if (width != null && height != null) {
        FreeformDevice(Size(width.toFloatInRange(), height.toFloatInRange()))
    } else {
        null
    }
}

private fun Int.toFloatInRange(): Float = toFloat().coerceIn(MinFreeformSize, MaxFreeformSize)

// What the link carries is only what differs from the initial state, so a fresh start has none.
internal fun encodeSettings(state: PreviewState, initial: PreviewState): String {
    val device = state.simulatedDevice.device
    val pairs = buildList {
        if (device != initial.simulatedDevice.device) {
            addAll(devicePairs(device))
        }
        addAll(configurationPairs(state.simulatedDevice.configuration, initial.simulatedDevice.configuration))
        addAll(previewSettingsPairs(state.previewSettings, initial.previewSettings))
    }
    return pairs.joinToString("&") { (key, value) -> "$KeyPrefix$key=$value" }
}

// Each pair is judged on its own: an invalid value or unknown key leaves that setting as it is in the initial state.
internal fun decodeSettings(encoded: String, initial: PreviewState): PreviewState {
    val values = settingValues(encoded)
    val device = values.device() ?: initial.simulatedDevice.device
    val configuration = values.configuration(initial.simulatedDevice.configuration)
    // The display size is snapped to the device's own steps here, and a keyboard it hasn't got dropped.
    return PreviewState(SimulatedDevice(device, configuration).adaptedToDevice(), values.previewSettings(initial.previewSettings))
}

internal fun settingsParams(query: String): String? =
    queryPairs(query).filter(::isSettingsPair).joinToString("&").ifEmpty { null }

internal fun withSettingsParams(query: String, encoded: String): String {
    val pairs = queryPairs(query).filterNot(::isSettingsPair) + queryPairs(encoded)
    return if (pairs.isEmpty()) "" else pairs.joinToString("&", prefix = "?")
}

private fun configurationPairs(configuration: DeviceConfiguration, initial: DeviceConfiguration) = buildList {
    if (configuration.orientation != initial.orientation) {
        add(OrientationKey to configuration.orientation.linkValue)
    }
    if (configuration.appearance != initial.appearance) {
        add(AppearanceKey to configuration.appearance.linkValue)
    }
    if (configuration.fontScale != initial.fontScale) {
        add(FontScaleKey to formatScale(configuration.fontScale))
    }
    if (configuration.displayScale != initial.displayScale) {
        add(DisplayScaleKey to formatScale(configuration.displayScale))
    }
    if (configuration.layoutDirection != initial.layoutDirection) {
        add(DirectionKey to (configuration.layoutDirection?.linkValue ?: Auto))
    }
    addFlag(KeyboardKey, configuration.isKeyboardVisible, initial.isKeyboardVisible)
    addFlag(BezelKey, configuration.isFrameVisible, initial.isFrameVisible)
    addFlag(SystemUiKey, configuration.isSystemUiVisible, initial.isSystemUiVisible)
}

private fun previewSettingsPairs(settings: PreviewSettings, initial: PreviewSettings) = buildList {
    if (settings.backdrop != initial.backdrop) {
        add(BackdropKey to settings.backdrop.linkValue)
    }
    addFlag(DotGridKey, settings.isDotGridVisible, initial.isDotGridVisible)
    addFlag(PreviewKey, settings.isPreviewModeEnabled, initial.isPreviewModeEnabled)
}

private fun queryPairs(query: String): List<String> = query.removePrefix("?").split('&').filter(String::isNotEmpty)

private fun isSettingsPair(pair: String): Boolean = pair.substringBefore('=').percentDecoded().startsWith(KeyPrefix)

private fun settingValues(encoded: String): Map<String, String> = buildMap {
    for (pair in queryPairs(encoded)) {
        val key = pair.substringBefore('=').percentDecoded()
        if ('=' in pair && key.startsWith(KeyPrefix)) {
            put(key.removePrefix(KeyPrefix), pair.substringAfter('=').percentDecoded())
        }
    }
}

private fun devicePairs(device: Device): List<Pair<String, String>> = when {
    device is FreeformDevice ->
        listOf(DeviceKey to Freeform, SizeKey to "${device.size.width.roundToInt()}x${device.size.height.roundToInt()}")
    device is DevicePreset && DevicePresets.byId(device.id) == device -> listOf(DeviceKey to device.id)
    else -> emptyList()
}

private fun formatScale(scale: Float): String {
    val hundredths = (scale * 100).roundToInt()
    val fraction = (hundredths % 100).toString().padStart(2, '0').trimEnd('0')
    return if (fraction.isEmpty()) "${hundredths / 100}" else "${hundredths / 100}.$fraction"
}
