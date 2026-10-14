@file:Suppress("FILE_NAME_MATCH_CLASS")

// Named for the theme; SegmentOption is the only class in it.

package com.apadmi.cmpdevicepreview.preview

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlin.math.abs
import kotlin.math.roundToInt

private const val DisabledAlpha = 0.45f

private const val MaxTicks = 12

private val TickRadius = 1.dp
private val MarkRadius = 2.dp

// M3 insets a slider's ticks from the ends of its track by the thumb gap and half the thumb.
private val TickInset = 8.dp

private val PanelRippleAlpha = RippleAlpha(
    draggedAlpha = 0.16f,
    focusedAlpha = 0.2f,
    hoveredAlpha = 0.14f,
    pressedAlpha = 0.16f,
)

internal val PanelWidth = 300.dp

/** The colours of the configuration panel, the device picker and the preview's handles. */
internal object PanelColors {
    val Background = Color(0xFF111114)
    val Surface = Color(0xFF1B1B20)
    val SurfaceRaised = Color(0xFF26262D)
    val Border = Color(0xFF2A2A31)
    val TextPrimary = Color(0xFFF4F4F6)
    val TextSecondary = Color(0xFFB0B0BA)
    val TextTertiary = Color(0xFF8A8A94)
    val Accent = Color(0xFF7C6CFF)
    val AccentBright = Color(0xFF9F94FF)
    val AccentSoft = Color(0x297C6CFF)

    /** The far end of gradients that start at [Accent]. */
    val AccentBlue = Color(0xFF4FA3FF)

    /** The accent for a fill that white text sits on. */
    val AccentDeep = Color(0xFF5B4BE0)
    val SwitchOff = Color(0xFF60606B)
    val SegmentSelected = Color(0xFF303039)
    val ThumbnailBody = Color(0xFF5A5A66)
    val ThumbnailScreenTop = Color(0xFF24242B)
    val ThumbnailScreenBottom = Color(0xFF32323B)

    /** A plain window's border, and the resize handles at rest. */
    val FrameOutline = Color(0xFF80808C)
}

/**
 * One option of a [SegmentedControl].
 *
 * @property label The option's name.
 * @property icon The icon shown beside the name.
 */
internal class SegmentOption(val label: String, val icon: ImageVector)

/** A setting's icon, title, note and badge, filling the row but for what follows them. */
@Composable
private fun RowScope.SettingLabel(
    icon: ImageVector,
    title: String,
    note: String?,
    badge: String?,
    titleEnd: (@Composable () -> Unit)? = null,
) {
    Box(
        Modifier.size(32.dp).background(PanelColors.Surface, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = PanelColors.TextSecondary, modifier = Modifier.size(18.dp))
    }
    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = PanelColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (badge != null) {
                Text(
                    badge,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(PanelColors.AccentSoft, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    color = PanelColors.AccentBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            titleEnd?.invoke()
        }
        if (note != null) {
            Text(
                note,
                color = PanelColors.TextTertiary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun DrawScope.isMarked(
    x: Float,
    marks: Set<Int>,
    lastIndex: Int
): Boolean {
    val inset = TickInset.toPx()
    val spacing = (size.width - 2 * inset) / lastIndex
    return marks.any { abs(inset + it * spacing - x) < spacing / 2 }
}

/**
 * The panel's dark Material theme, so ripples, text fields and dialogs inside it match.
 *
 * @param content What is shown inside the theme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PanelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = PanelColors.Accent,
            onPrimary = Color.White,
            background = PanelColors.Background,
            onBackground = PanelColors.TextPrimary,
            surface = PanelColors.Surface,
            onSurface = PanelColors.TextPrimary,
            surfaceVariant = PanelColors.SurfaceRaised,
            onSurfaceVariant = PanelColors.TextSecondary,
            outline = PanelColors.Border,
            outlineVariant = PanelColors.Border,
            surfaceContainerHigh = PanelColors.Surface,
        ),
        content = {
            CompositionLocalProvider(
                LocalRippleConfiguration provides RippleConfiguration(PanelColors.TextPrimary, PanelRippleAlpha),
                content = content,
            )
        },
    )
}

/**
 * A small capitalised heading above a group of controls.
 *
 * @param text The heading, shown in upper case.
 * @param modifier The modifier for the heading.
 * @param trailing Shown after the heading, such as a reset button.
 */
@Composable
internal fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(modifier.padding(start = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text.uppercase(),
            color = PanelColors.TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.9.sp,
        )
        trailing?.invoke()
    }
}

/**
 * A short note below a control.
 *
 * @param text The note.
 */
@Composable
internal fun PanelCaption(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        color = PanelColors.TextTertiary,
        fontSize = 12.sp,
    )
}

/**
 * Equal-width options on a track, with an indicator that slides to the selected one.
 *
 * @param options The options, in order, each with a label and an icon.
 * @param selectedIndex The index in [options] of the selected option.
 * @param onSelect Called with the index of the option that is pressed.
 * @param modifier The modifier for the track.
 * @param enabled Whether the options can be pressed. When false the control is dimmed.
 */
@Composable
internal fun SegmentedControl(
    options: List<SegmentOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(PanelColors.Surface)
            .border(1.dp, PanelColors.Border, shape)
            .padding(3.dp)
            .alpha(if (enabled) 1f else DisabledAlpha)
    ) {
        val segmentWidth = maxWidth / options.size
        val indicatorOffset by animateDpAsState(segmentWidth * selectedIndex, label = "segment")
        Box(
            Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .background(PanelColors.SegmentSelected, RoundedCornerShape(8.dp))
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                val color by animateColorAsState(
                    if (selected) PanelColors.TextPrimary else PanelColors.TextSecondary,
                    label = "segmentColor",
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(selected = selected, enabled = enabled, role = Role.RadioButton) { onSelect(index) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(option.icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Text(
                        option.label,
                        modifier = Modifier.padding(start = 6.dp),
                        color = color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * A compact switch; the row around it handles the toggle.
 *
 * @param checked Whether the switch is on.
 * @param modifier The modifier for the switch.
 */
@Composable
internal fun PanelSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val track by animateColorAsState(if (checked) PanelColors.Accent else PanelColors.SwitchOff, label = "track")
    val thumbOffset by animateDpAsState(if (checked) 16.dp else 0.dp, label = "thumb")
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 20.dp)
            .background(track, CircleShape)
            .padding(2.dp)
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .shadow(1.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

/**
 * A setting that is on or off: an icon, a title, an optional note and badge, and a switch.
 *
 * @param icon The icon beside the title.
 * @param title The name of the setting.
 * @param checked Whether the setting is on.
 * @param onCheckedChange Called with the new value when the row is toggled.
 * @param modifier The modifier for the row.
 * @param note A line below the title that says more about the setting, or null for none.
 * @param badge A short label beside the title, or null for none.
 * @param enabled Whether the row can be toggled. When false it is dimmed.
 */
@Composable
internal fun ToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    badge: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .alpha(if (enabled) 1f else DisabledAlpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingLabel(icon, title, note, badge)
        PanelSwitch(checked = checked)
    }
}

/**
 * A setting with a few values in order, such as a text size: an icon, a title, an optional note,
 * the selected value's label as a badge, and a slider that stops at each of [labels].
 *
 * @param icon The icon beside the title.
 * @param title The name of the setting.
 * @param labels The label of each value, in order.
 * @param selectedIndex The index in [labels] of the selected value.
 * @param onSelect Called with the index of the value the slider moves to.
 * @param modifier The modifier for the setting.
 * @param note A line below the title that says more about the setting, or null for none.
 * @param defaultIndex The index in [labels] a reset button returns to, or null for no button.
 * @param markedIndices The indices in [labels] to put a tick at, when there are too many labels for a tick each.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StepSlider(
    icon: ImageVector,
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    defaultIndex: Int? = null,
    markedIndices: Set<Int> = emptySet(),
) {
    val colors = SliderDefaults.colors(
        thumbColor = PanelColors.Accent,
        activeTrackColor = PanelColors.Accent,
        activeTickColor = PanelColors.AccentBright,
        inactiveTrackColor = PanelColors.SwitchOff,
        inactiveTickColor = PanelColors.TextSecondary,
    )
    Column(modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingLabel(
                icon,
                title,
                note,
                badge = labels.getOrNull(selectedIndex),
                titleEnd = defaultIndex?.let {
                    { ResetButton(title, isShown = selectedIndex != it, onClick = { onSelect(it) }) }
                },
            )
        }
        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { value ->
                value.roundToInt().let {
                    if (it != selectedIndex) {
                        onSelect(it)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .pointerHoverIcon(PointerIcon.Hand)
                .semantics {
                    contentDescription = title
                    labels.getOrNull(selectedIndex)?.let { stateDescription = it }
                },
            enabled = labels.size > 1,
            valueRange = 0f..(labels.size - 1).coerceAtLeast(1).toFloat(),
            steps = (labels.size - 2).coerceAtLeast(0),
            colors = colors,
            // A tick for each value while there are few enough to tell apart, else only the marked ones.
            track = { state ->
                SliderDefaults.Track(
                    state,
                    colors = colors,
                    drawTick = { center, color ->
                        val isEachTicked = labels.size <= MaxTicks
                        if (isEachTicked) {
                            drawCircle(color, radius = TickRadius.toPx(), center = center)
                        } else if (isMarked(center.x, markedIndices, labels.lastIndex)) {
                            drawCircle(color, radius = MarkRadius.toPx(), center = center)
                        }
                    },
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PanelTooltip(
    text: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip(containerColor = PanelColors.SurfaceRaised, contentColor = PanelColors.TextPrimary) {
                Text(text)
            }
        },
        state = rememberTooltipState(),
        modifier = modifier,
        enableUserInput = isEnabled,
        content = content,
    )
}

@Composable
internal fun PanelIconButton(
    icon: ImageVector,
    description: String,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = PanelColors.TextSecondary,
    isShown: Boolean = true,
    enabled: Boolean = true,
) {
    val alpha by animateFloatAsState(if (!isShown) {
        0f
    } else if (enabled) {
        1f
    } else {
        DisabledAlpha
    }, label = "iconAlpha")
    val interactions = remember { MutableInteractionSource() }
    val isHovered by interactions.collectIsHoveredAsState()
    PanelTooltip(tooltip, isEnabled = isShown) {
        Box(
            modifier
                .size(size)
                .alpha(alpha)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactions,
                    indication = LocalIndication.current,
                    enabled = isShown && enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .pointerHoverIcon(PointerIcon.Hand),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (isHovered && enabled) PanelColors.TextPrimary else tint,
                modifier = Modifier.size(size * 0.65f),
            )
        }
    }
}

@Composable
internal fun ResetButton(
    title: String,
    isShown: Boolean,
    onClick: () -> Unit
) {
    PanelIconButton(
        icon = Icons.Outlined.RestartAlt,
        description = "Reset $title",
        tooltip = "Reset to default",
        onClick = onClick,
        modifier = Modifier.padding(start = 4.dp),
        size = 18.dp,
        tint = PanelColors.TextTertiary,
        isShown = isShown,
    )
}
