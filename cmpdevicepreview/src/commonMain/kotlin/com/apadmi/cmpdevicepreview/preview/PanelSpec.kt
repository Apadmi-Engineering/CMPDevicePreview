package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.apadmi.cmpdevicepreview.core.FontScaleSteps
import com.apadmi.cmpdevicepreview.core.ScaleStep

internal val DefaultPanel: PanelScope.() -> Unit = { defaults() }

private val DefaultBody: List<PanelEntry> = listOf(
    PanelEntry.Control(PanelControl.Frame),
    PanelEntry.Control(PanelControl.Orientation),
    PanelEntry.Control(PanelControl.Appearance),
    PanelEntry.Section("Text & display", listOf(PanelEntry.Control(PanelControl.TextSize), PanelEntry.Control(PanelControl.DisplaySize))),
    PanelEntry.Control(PanelControl.LayoutDirection),
    PanelEntry.Control(PanelControl.Language),
    PanelEntry.Section(
        "Display",
        listOf(PanelEntry.Control(PanelControl.Bezel), PanelEntry.Control(PanelControl.SystemUi), PanelEntry.Control(PanelControl.Keyboard)),
    ),
    PanelEntry.Control(PanelControl.Backdrop),
    PanelEntry.Control(PanelControl.DotGrid),
    PanelEntry.Control(PanelControl.PreviewMode),
)

/**
 * The panel's own controls. A row draws its title in its row, so it can go in a section; the rest
 * show their name as a heading.
 *
 * @property isRow Whether the control is a row.
 */
internal enum class PanelControl(val isRow: Boolean = false) {
    Header,
    Frame,
    Orientation,
    Appearance,
    TextSize(isRow = true),
    DisplaySize(isRow = true),
    LayoutDirection,
    Language,
    Bezel(isRow = true),
    SystemUi(isRow = true),
    Keyboard(isRow = true),
    Backdrop,
    DotGrid(isRow = true),
    PreviewMode(isRow = true),
    Attribution,
    ;

    val dslName: String
        get() = name.replaceFirstChar(Char::lowercaseChar)
}

internal sealed interface PanelEntry {
    /**
     * One of the panel's own controls.
     *
     * @property control The control.
     */
    data class Control(val control: PanelControl) : PanelEntry

    /**
     * A setting of your own.
     *
     * @property setting The setting.
     */
    data class Setting(val setting: PanelSetting<*>) : PanelEntry

    /**
     * Anything else.
     *
     * @property content What is shown.
     */
    data class Item(val content: @Composable () -> Unit) : PanelEntry

    /**
     * Entries under a heading of their own.
     *
     * @property title The heading.
     * @property entries The entries, in order.
     */
    data class Section(val title: String, val entries: List<PanelEntry>) : PanelEntry
}

/**
 * The panel's header.
 *
 * @property title The title.
 * @property icon The icon beside the title.
 */
internal data class PanelHeader(val title: String, val icon: @Composable () -> Unit)

/**
 * A panel as its DSL describes it, which the view draws and the preview's state is fitted to.
 *
 * @property header The header, or null for none.
 * @property entries The settings between the header and the attribution, in order.
 * @property controls The panel's own controls that are shown, anywhere in it: every other one is hidden.
 * @property textSizeSteps The text sizes the text size slider offers.
 * @property settings The settings of your own that are placed in the panel.
 */
internal data class PanelSpec(
    val header: PanelHeader?,
    val entries: List<PanelEntry>,
    val controls: Set<PanelControl>,
    val textSizeSteps: List<ScaleStep>,
    val settings: List<PanelSetting<*>>,
)

private class PanelPlacements {
    val controls: MutableSet<PanelControl> = mutableSetOf()
    val settings: MutableList<PanelSetting<*>> = mutableListOf()
    var textSizeSteps: List<ScaleStep>? = null

    fun place(control: PanelControl) {
        require(controls.add(control)) { "${control.dslName}() is placed in the panel twice" }
    }

    fun place(setting: PanelSetting<*>) {
        require(settings.none { it.key == setting.key }) { "Two settings in the panel have the key \"${setting.key}\"" }
        settings += setting
    }
}

/**
 * Collects the entries of the panel or of a section.
 *
 * @property placements What is placed in the whole panel, so nothing is placed twice.
 */
private abstract class EntriesBuilder(protected val placements: PanelPlacements) : PanelSectionScope {
    val entries: MutableList<PanelEntry> = mutableListOf()

    override fun textSize(range: ClosedFloatingPointRange<Float>) {
        placements.textSizeSteps = textSizeStepsIn(range)
        add(PanelControl.TextSize)
    }

    override fun displaySize() = add(PanelControl.DisplaySize)

    override fun bezel() = add(PanelControl.Bezel)

    override fun systemUi() = add(PanelControl.SystemUi)

    override fun keyboard() = add(PanelControl.Keyboard)

    override fun dotGrid() = add(PanelControl.DotGrid)

    override fun previewMode() = add(PanelControl.PreviewMode)

    override fun setting(setting: PanelSetting<*>) {
        placements.place(setting)
        entries += PanelEntry.Setting(setting)
    }

    override fun item(content: @Composable () -> Unit) {
        entries += PanelEntry.Item(content)
    }

    protected fun add(control: PanelControl) {
        placements.place(control)
        entries += PanelEntry.Control(control)
    }
}

private class SectionBuilder(placements: PanelPlacements) : EntriesBuilder(placements)

private class PanelBuilder : EntriesBuilder(PanelPlacements()), PanelScope {
    private var header: PanelHeader? = null
    private var defaultOptions: DefaultPanelOptions? = null
    private var defaultsIndex = 0

    override fun defaults(configure: DefaultPanelScope.() -> Unit) {
        require(defaultOptions == null) { "defaults() is called twice in the panel" }
        defaultOptions = DefaultPanelOptions().apply(configure)
        defaultsIndex = entries.size
    }

    override fun header(title: String, icon: @Composable () -> Unit) {
        placements.place(PanelControl.Header)
        header = PanelHeader(title, icon)
    }

    override fun section(title: String, content: PanelSectionScope.() -> Unit) {
        entries += PanelEntry.Section(title, SectionBuilder(placements).apply(content).entries)
    }

    override fun frame() = add(PanelControl.Frame)

    override fun orientation() = add(PanelControl.Orientation)

    override fun appearance() = add(PanelControl.Appearance)

    override fun layoutDirection() = add(PanelControl.LayoutDirection)

    override fun language() = add(PanelControl.Language)

    override fun backdrop() = add(PanelControl.Backdrop)

    override fun attribution() = placements.place(PanelControl.Attribution)

    fun build(): PanelSpec {
        val placed = placements.controls.toSet()
        val options = defaultOptions
        options?.requireUnchanged(placed)
        val fromDefaults = options?.shown(except = placed).orEmpty()
        val body = if (options == null) {
            entries.toList()
        } else {
            entries.take(defaultsIndex) + DefaultBody.including(fromDefaults) + entries.drop(defaultsIndex)
        }
        return PanelSpec(
            header = header ?: options?.header?.takeIf { PanelControl.Header in fromDefaults }?.let { PanelHeader(it.title, it.icon) },
            entries = body,
            controls = placed + fromDefaults,
            textSizeSteps = placements.textSizeSteps ?: textSizeStepsIn(options?.textSize?.range ?: DefaultTextSizeRange),
            settings = placements.settings.toList(),
        )
    }
}

private open class ControlOptions : PanelControlOptions {
    var isChanged: Boolean = false
        protected set
    override var isVisible: Boolean = true
        set(value) {
            field = value
            isChanged = true
        }
}

private class TextSizeOptions : ControlOptions(), PanelTextSizeOptions {
    override var range: ClosedFloatingPointRange<Float> = DefaultTextSizeRange
        set(value) {
            field = value
            isChanged = true
        }
}

private class HeaderOptions : ControlOptions(), PanelHeaderOptions {
    override var title: String = DefaultPanelTitle
        set(value) {
            field = value
            isChanged = true
        }
    override var icon: @Composable () -> Unit = DefaultPanelIcon
        set(value) {
            field = value
            isChanged = true
        }
}

private class DefaultPanelOptions : DefaultPanelScope {
    override val header: HeaderOptions = HeaderOptions()
    override val frame: ControlOptions = ControlOptions()
    override val orientation: ControlOptions = ControlOptions()
    override val appearance: ControlOptions = ControlOptions()
    override val textSize: TextSizeOptions = TextSizeOptions()
    override val displaySize: ControlOptions = ControlOptions()
    override val layoutDirection: ControlOptions = ControlOptions()
    override val language: ControlOptions = ControlOptions()
    override val bezel: ControlOptions = ControlOptions()
    override val systemUi: ControlOptions = ControlOptions()
    override val keyboard: ControlOptions = ControlOptions()
    override val backdrop: ControlOptions = ControlOptions()
    override val dotGrid: ControlOptions = ControlOptions()
    override val previewMode: ControlOptions = ControlOptions()
    override val attribution: ControlOptions = ControlOptions()
    private val byControl: Map<PanelControl, ControlOptions> = mapOf(
        PanelControl.Header to header,
        PanelControl.Frame to frame,
        PanelControl.Orientation to orientation,
        PanelControl.Appearance to appearance,
        PanelControl.TextSize to textSize,
        PanelControl.DisplaySize to displaySize,
        PanelControl.LayoutDirection to layoutDirection,
        PanelControl.Language to language,
        PanelControl.Bezel to bezel,
        PanelControl.SystemUi to systemUi,
        PanelControl.Keyboard to keyboard,
        PanelControl.Backdrop to backdrop,
        PanelControl.DotGrid to dotGrid,
        PanelControl.PreviewMode to previewMode,
        PanelControl.Attribution to attribution,
    )

    fun shown(except: Set<PanelControl>): Set<PanelControl> =
        byControl.filter { (control, options) -> control !in except && options.isVisible }.keys

    // An option for a control placed elsewhere would be silently ignored.
    fun requireUnchanged(placed: Set<PanelControl>) {
        for (control in placed) {
            require(!byControl.getValue(control).isChanged) {
                "${control.dslName} is placed in the panel, so set its options where it is placed rather than in defaults()"
            }
        }
    }
}

private fun List<PanelEntry>.including(controls: Set<PanelControl>): List<PanelEntry> = mapNotNull { entry ->
    when (entry) {
        is PanelEntry.Control -> entry.takeIf { it.control in controls }
        is PanelEntry.Section -> entry.entries.including(controls)
            .takeIf { it.isNotEmpty() }
            ?.let { entry.copy(entries = it) }
        else -> entry
    }
}

internal fun buildPanel(content: PanelScope.() -> Unit): PanelSpec = PanelBuilder().apply(content).build()

// As LazyColumn does with its content, the panel is built again when a state the block reads changes, not only
// when the block does.
@Composable
internal fun rememberPanelSpec(panel: PanelScope.() -> Unit): State<PanelSpec> {
    val latest = rememberUpdatedState(panel)
    return remember { derivedStateOf { buildPanel(latest.value) } }
}

private fun textSizeStepsIn(range: ClosedFloatingPointRange<Float>): List<ScaleStep> =
    FontScaleSteps.filter { it.scale in range }.also { steps ->
        require(steps.isNotEmpty()) { "The text size range $range holds none of the sizes the slider offers, 50% to 300% in steps of 5%" }
    }
