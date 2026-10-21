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
private const val LanguageKey = "language"
private const val KeyboardKey = "keyboard"
private const val BezelKey = "bezel"
private const val SystemUiKey = "systemUi"
private const val BackdropKey = "backdrop"
private const val DotGridKey = "dotGrid"
private const val PreviewKey = "preview"
private const val SettingKeyPrefix = "x."
private const val UnreservedPunctuation = "-._~"

private const val Freeform = "freeform"
private const val Auto = "auto"
internal const val FlagOn = "1"
internal const val FlagOff = "0"

private val PercentEscapes = Regex("(%[0-9a-fA-F]{2})+")

// Only the shape of a BCP 47 tag: which tags the host offers is judged where they are known.
private val LanguageTag = Regex("[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8}){0,3}")

internal val TrueValues = setOf(FlagOn, "true", "on")
internal val FalseValues = setOf(FlagOff, "false", "off")

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
    language = language(initial.language),
)

private fun Map<String, String>.previewSettings(initial: PreviewSettings) = PreviewSettings(
    backdrop = enumValue<Backdrop>(BackdropKey) ?: initial.backdrop,
    isDotGridVisible = flag(DotGridKey) ?: initial.isDotGridVisible,
    isPreviewModeEnabled = flag(PreviewKey) ?: initial.isPreviewModeEnabled,
)

// A run of escapes is the UTF-8 of what was escaped, so a name such as "Português" comes back as written.
private fun String.percentDecoded(): String = if ('%' in this) {
    PercentEscapes.replace(this) { run ->
        run.value.chunked(3).map { it.substring(1).toInt(16).toByte() }.toByteArray()
            .decodeToString()
    }
} else {
    this
}

// RFC 3986's unreserved characters stay and every other UTF-8 byte is escaped, the same on every platform.
private fun String.percentEncoded(): String = buildString {
    for (byte in this@percentEncoded.encodeToByteArray()) {
        val char = byte.toInt().toChar()
        if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in UnreservedPunctuation) {
            append(char)
        } else {
            append('%').append(byte.toHexString(HexFormat.UpperCase))
        }
    }
}

private fun MutableList<Pair<String, String>>.addFlag(
    key: String,
    value: Boolean,
    initial: Boolean
) {
    if (value != initial) {
        add(key to if (value) FlagOn else FlagOff)
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

private fun Map<String, String>.language(initial: String?): String? {
    val value = this[LanguageKey] ?: return initial
    return when {
        value.equals(Auto, ignoreCase = true) -> null
        LanguageTag.matches(value) -> value
        else -> initial
    }
}

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
        // The values a panel keeps are only those that differ from the settings' defaults.
        state.settingValues.values.forEach { (key, value) -> add("$SettingKeyPrefix$key" to value.percentEncoded()) }
    }
    return pairs.joinToString("&") { (key, value) -> "$KeyPrefix$key=$value" }
}

// Each pair is judged on its own: an invalid value or unknown key leaves that setting as it is in the initial state.
internal fun decodeSettings(encoded: String, initial: PreviewState): PreviewState {
    val values = linkValues(encoded)
    val device = values.device() ?: initial.simulatedDevice.device
    val configuration = values.configuration(initial.simulatedDevice.configuration)
    // Which keys are settings of the panel, and which values their options, is judged where the panel is known.
    val settingValues = values.filterKeys { it.startsWith(SettingKeyPrefix) }.mapKeys { it.key.removePrefix(SettingKeyPrefix) }
    // The display size is snapped to the device's own steps here, and a keyboard it hasn't got dropped.
    return PreviewState(
        SimulatedDevice(device, configuration).adaptedToDevice(),
        values.previewSettings(initial.previewSettings),
        PanelSettingValues(settingValues),
    )
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
    if (configuration.language != initial.language) {
        add(LanguageKey to (configuration.language ?: Auto))
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

private fun linkValues(encoded: String): Map<String, String> = buildMap {
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
