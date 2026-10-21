package com.apadmi.cmpdevicepreview.preview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector

private val SettingKey = Regex("[A-Za-z0-9_-]+")

internal val LocalPanelSettingValues = compositionLocalOf { PanelSettingValues() }

/**
 * A setting of your own in the configuration panel, such as a feature flag or a choice of
 * environment. Place it with [PanelSectionScope.setting]. Like the panel's own settings, its value
 * is kept across reloads, carried in a link as `dp.x.<key>`, and returned to [default] by the
 * panel's reset button.
 *
 * Declare it once, outside composition, and read [current] inside the device:
 * ```
 * val MockApi = PanelSetting.toggle(key = "mockApi", title = "Mock API")
 *
 * DeviceWithConfigurationView(panel = { defaults(); setting(MockApi) }) {
 *     App(api = if (MockApi.current) FakeApi() else RealApi())
 * }
 * ```
 *
 * @property key What the setting is kept and linked under: letters, digits, `_` and `-`, unique in its panel.
 * @property title The name the panel shows for the setting.
 * @property default The value the setting starts with and is reset to.
 * @property options The values the setting can have; a toggle's are false then true.
 * @property isToggle Whether the setting is a switch rather than a menu.
 * @property note A line that says more about the setting, or null for none.
 * @property icon The icon the panel shows with the setting.
 */
public class PanelSetting<T> private constructor(
    public val key: String,
    public val title: String,
    public val default: T,
    internal val options: List<T>,
    internal val isToggle: Boolean,
    internal val note: String?,
    internal val icon: ImageVector,
    private val labelOf: (T) -> String,
) {
    /**
     * The setting's value in the panel, inside `DeviceWithConfigurationView`, or [default] outside it.
     */
    public val current: T
        @Composable
        @ReadOnlyComposable
        get() = LocalPanelSettingValues.current[this]

    init {
        require(SettingKey.matches(key)) { "A setting's key must be letters, digits, _ and -, so a link can carry it: \"$key\"" }
        require(options.isNotEmpty()) { "The setting \"$key\" has no options" }
        require(default in options) { "The default of the setting \"$key\" isn't one of its options: $default" }
        val linkValues = options.map { linkValue(it).lowercase() }
        require(linkValues.distinct().size == linkValues.size) { "The options of the setting \"$key\" need labels that differ, ignoring case" }
    }

    internal fun label(value: T): String = labelOf(value)

    internal fun linkValue(value: T): String = if (isToggle) {
        if (value == true) FlagOn else FlagOff
    } else {
        labelOf(value)
    }

    // A toggle's options are false then true, and a link writes it as a flag, so it reads one in any of their forms.
    internal fun optionIndex(linkValue: String): Int? {
        val index = if (isToggle) {
            when (linkValue.lowercase()) {
                in TrueValues -> 1
                in FalseValues -> 0
                else -> -1
            }
        } else {
            options.indexOfFirst { labelOf(it).equals(linkValue, ignoreCase = true) }
        }
        return index.takeIf { it >= 0 }
    }

    // The value as a link writes it, or null when it isn't one of the options or is the default, which a link leaves out.
    internal fun canonical(linkValue: String): String? =
        optionIndex(linkValue)?.let(options::get)?.takeIf { it != default }?.let(::linkValue)

    /** Makes the settings you place in the panel. */
    public companion object {
        /**
         * A setting that is on or off, shown as a switch.
         *
         * @param key What the setting is kept and linked under: letters, digits, `_` and `-`, unique in its panel.
         * @param title The name the panel shows for the setting.
         * @param default Whether the setting starts on.
         * @param note A line below the title that says more about the setting, or null for none.
         * @param icon The icon beside the title.
         * @return The setting, to place in the panel and read inside the device.
         */
        public fun toggle(
            key: String,
            title: String,
            default: Boolean = false,
            note: String? = null,
            icon: ImageVector = Icons.Outlined.Tune,
        ): PanelSetting<Boolean> = PanelSetting(key, title, default, listOf(false, true), isToggle = true, note, icon) { it.toString() }

        /**
         * A setting with one of a few values, shown as a menu.
         *
         * @param key What the setting is kept and linked under: letters, digits, `_` and `-`, unique in its panel.
         * @param title The name the panel shows for the setting.
         * @param options The values to choose from, in the order the menu shows them.
         * @param default The value the setting starts with, one of [options].
         * @param note A line below the menu that says more about the setting, or null for none.
         * @param icon The icon beside the chosen value.
         * @param label The name the menu shows for a value, which a link also carries, so each must
         * differ from the others ignoring case.
         * @return The setting, to place in the panel and read inside the device.
         */
        @Suppress("TOO_MANY_PARAMETERS")  // Each is named, and all but the first three have defaults.
        public fun <T> choice(
            key: String,
            title: String,
            options: List<T>,
            default: T = options.first(),
            note: String? = null,
            icon: ImageVector = Icons.Outlined.Tune,
            label: (T) -> String = { it.toString() },
        ): PanelSetting<T> = PanelSetting(key, title, default, options, isToggle = false, note, icon, label)
    }
}

/**
 * The values of the [PanelSetting]s in a panel, for when you place `ConfigurationView` yourself and
 * hold its state. Each setting not given a value has its default.
 *
 * @property values Each setting's value as a link writes it, by key, leaving out those at their defaults.
 */
public class PanelSettingValues internal constructor(internal val values: Map<String, String>) {
    /** Every setting at its default. */
    public constructor() : this(emptyMap())

    /**
     * The value of [setting].
     *
     * @param setting The setting to read.
     * @return Its value, or its default when it has none.
     */
    public operator fun <T> get(setting: PanelSetting<T>): T =
        values[setting.key]?.let(setting::optionIndex)?.let(setting.options::get) ?: setting.default

    /**
     * A copy with [setting] set to [value].
     *
     * @param setting The setting to change.
     * @param value Its new value, one of its options.
     * @return These values with [setting] changed.
     */
    public fun <T> with(setting: PanelSetting<T>, value: T): PanelSettingValues =
        PanelSettingValues(if (value == setting.default) values - setting.key else values + (setting.key to setting.linkValue(value)))

    // Keeps only the values of these settings that are options of them, as a link writes them.
    internal fun fittedTo(settings: List<PanelSetting<*>>): PanelSettingValues {
        val fitted = settings.mapNotNull { setting -> values[setting.key]?.let(setting::canonical)?.let { setting.key to it } }.toMap()
        return if (fitted == values) this else PanelSettingValues(fitted)
    }

    override fun equals(other: Any?): Boolean = other is PanelSettingValues && other.values == values

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String = "PanelSettingValues($values)"
}
