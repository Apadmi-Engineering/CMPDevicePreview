package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.FontScaleSteps

internal val DefaultTextSizeRange: ClosedFloatingPointRange<Float> = FontScaleSteps.first().scale..FontScaleSteps.last().scale

/** Marks the scopes that build the configuration panel, so a block only offers what can go there. */
@DslMarker
public annotation class PanelDsl

/**
 * Where controls go: the panel itself, or a section of it. They are shown in the order they are
 * placed, and each can be placed once. A control placed nowhere is hidden, and its setting stays as
 * the host started it, whatever a link or the kept settings say.
 */
@PanelDsl
public sealed interface PanelSectionScope {
    /**
     * The text size slider, which sets [DeviceConfiguration.fontScale].
     *
     * @param range The font scales the slider offers, such as `0.85f..2f`. It must hold at least one
     * of its steps, which are 5% apart from 50% to 300%. The text size is kept within it.
     */
    public fun textSize(range: ClosedFloatingPointRange<Float> = DefaultTextSizeRange)

    /** The display size slider, which offers the sizes the device has. */
    public fun displaySize()

    /** The switch that shows the device's bezel, or a desktop window's border. */
    public fun bezel()

    /** The switch that shows the status bar and gesture bar, where the device has them. */
    public fun systemUi()

    /** The switch that raises the software keyboard, where the device has one. */
    public fun keyboard()

    /** The switch that shows the grid of dots behind the device. */
    public fun dotGrid()

    /**
     * A setting of your own, shown like the panel's.
     *
     * @param setting The setting, whose key must differ from those of the panel's other settings.
     */
    public fun setting(setting: PanelSetting<*>)

    /**
     * Anything else, such as a button, as wide as the panel's settings and inside its dark Material theme.
     *
     * @param content What is shown.
     */
    public fun item(content: @Composable () -> Unit)
}

/**
 * The configuration panel, from its header to its footer. Write it out in full, or call [defaults]
 * for the usual panel and add to it:
 * ```
 * panel = {
 *     defaults {
 *         backdrop.isVisible = false
 *         textSize.range = 0.85f..2f
 *     }
 *     section("Debug") {
 *         setting(MockApi)
 *     }
 * }
 * ```
 * The controls that show their name as a heading, such as [appearance], can only go here, not in a
 * [section].
 */
@PanelDsl
public sealed interface PanelScope : PanelSectionScope {
    /**
     * The usual panel, here: every control that isn't placed elsewhere in this panel, in the usual
     * order. Called with no block, it is the whole of the default panel.
     *
     * @param configure Hides the usual controls or changes their options.
     */
    public fun defaults(configure: DefaultPanelScope.() -> Unit = {})

    /**
     * The header at the top of the panel, wherever it is placed, with the button that resets every setting.
     *
     * @param title The title in the header.
     * @param icon The icon beside the title, which fills a square 28 dp across: the whole icon, with
     * any background of its own. It is read as decoration unless it sets a content description.
     */
    public fun header(title: String = DefaultPanelTitle, icon: @Composable () -> Unit = DefaultPanelIcon)

    /**
     * Controls under a heading of their own.
     *
     * @param title The heading.
     * @param content The controls, in order.
     */
    public fun section(title: String, content: PanelSectionScope.() -> Unit)

    /** The device and the button that opens the picker. */
    public fun frame()

    /** The orientation, or for a freeform window, its width and height. */
    public fun orientation()

    /** Light or dark mode inside the device. */
    public fun appearance()

    /** Left to right or right to left inside the device. */
    public fun layoutDirection()

    /** The language inside the device, from the `languages` the host passes. Without any, it shows nothing. */
    public fun language()

    /** The colour behind the device. */
    public fun backdrop()

    /** The "Built with CMPDevicePreview" link to the project's GitHub repository, after the settings wherever it is placed. */
    public fun attribution()

    /**
     * The switch at the foot of the panel, wherever it is placed, that shows just the app. Left out,
     * preview mode stays as the host started it: started off, only the app is shown, with no way back
     * to the panel.
     */
    public fun previewMode()
}

/**
 * The usual panel's controls, each shown unless hidden here. A control placed elsewhere in the
 * panel is configured there instead.
 */
@PanelDsl
public sealed interface DefaultPanelScope {
    /** The header, with the button that resets every setting. */
    public val header: PanelHeaderOptions

    /** The device and the button that opens the picker. */
    public val frame: PanelControlOptions

    /** The orientation, or for a freeform window, its width and height. */
    public val orientation: PanelControlOptions

    /** Light or dark mode. */
    public val appearance: PanelControlOptions

    /** The text size slider. */
    public val textSize: PanelTextSizeOptions

    /** The display size slider. */
    public val displaySize: PanelControlOptions

    /** The layout direction. */
    public val layoutDirection: PanelControlOptions

    /** The language, shown when the host passes `languages`. */
    public val language: PanelControlOptions

    /** The bezel switch. */
    public val bezel: PanelControlOptions

    /** The system UI switch. */
    public val systemUi: PanelControlOptions

    /** The keyboard switch. */
    public val keyboard: PanelControlOptions

    /** The backdrop colour. */
    public val backdrop: PanelControlOptions

    /** The dot grid switch. */
    public val dotGrid: PanelControlOptions

    /** The link to the project's repository. */
    public val attribution: PanelControlOptions

    /** The preview mode switch at the foot of the panel: see [PanelScope.previewMode]. */
    public val previewMode: PanelControlOptions
}

/** Whether one of the usual panel's controls is shown. */
public sealed interface PanelControlOptions {
    /** Whether the control is shown. When false, its setting stays as the host started it. */
    public var isVisible: Boolean
}

/** The options of the usual panel's text size slider. */
public sealed interface PanelTextSizeOptions : PanelControlOptions {
    /** The font scales the slider offers: see [PanelSectionScope.textSize]. */
    public var range: ClosedFloatingPointRange<Float>
}

/** The options of the usual panel's header. */
public sealed interface PanelHeaderOptions : PanelControlOptions {
    /** The title in the header. */
    public var title: String

    /** The icon beside the title: see [PanelScope.header]. */
    public var icon: @Composable () -> Unit
}
