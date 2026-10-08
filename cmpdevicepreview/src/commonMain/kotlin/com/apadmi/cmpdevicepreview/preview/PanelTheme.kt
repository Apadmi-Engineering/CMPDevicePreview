package com.apadmi.cmpdevicepreview.preview

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** The colours of the configuration panel and the device picker. */
internal object PanelColors {
    val Background = Color(0xFF111114)
    val Surface = Color(0xFF1B1B20)
    val SurfaceRaised = Color(0xFF26262D)
    val Border = Color(0xFF2A2A31)
    val TextPrimary = Color(0xFFF4F4F6)
    val TextSecondary = Color(0xFF9D9DA8)
    val TextTertiary = Color(0xFF66666F)
    val Accent = Color(0xFF7C6CFF)
    val AccentBright = Color(0xFF9F94FF)
    val AccentSoft = Color(0x297C6CFF)
    val SwitchOff = Color(0xFF3A3A43)
    val SegmentSelected = Color(0xFF303039)
}

internal val PanelWidth = 300.dp

/** The panel's dark Material theme, so ripples, text fields and dialogs inside it match. */
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
        content = content,
    )
}

/** A small capitalised heading above a group of controls. */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
        color = PanelColors.TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.9.sp,
    )
}

internal class SegmentOption(val label: String, val icon: ImageVector)

/** Equal-width options on a track, with an indicator that slides to the selected one. */
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
            .alpha(if (enabled) 1f else 0.45f)
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

/** A compact switch; the row around it handles the toggle. */
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

/** A setting that is on or off: an icon, a title, an optional note and badge, and a switch. */
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
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingLabel(icon, title, note, badge)
        PanelSwitch(checked = checked)
    }
}

/**
 * A setting with a few values in order, such as a text size: an icon, a title, an optional note,
 * the selected value's label as a badge, and a slider that stops at each of [labels].
 */
@Composable
internal fun StepSlider(
    icon: ImageVector,
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingLabel(icon, title, note, badge = labels.getOrNull(selectedIndex))
        }
        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { value -> value.roundToInt().let { if (it != selectedIndex) onSelect(it) } },
            modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
            enabled = labels.size > 1,
            valueRange = 0f..(labels.size - 1).coerceAtLeast(1).toFloat(),
            steps = (labels.size - 2).coerceAtLeast(0),
            // Ticks only while there are few enough to tell apart.
            colors = SliderDefaults.colors(
                thumbColor = PanelColors.Accent,
                activeTrackColor = PanelColors.Accent,
                activeTickColor = if (labels.size <= MaxTicks) PanelColors.AccentBright else Color.Transparent,
                inactiveTrackColor = PanelColors.SwitchOff,
                inactiveTickColor = if (labels.size <= MaxTicks) PanelColors.TextTertiary else Color.Transparent,
            ),
        )
    }
}

private const val MaxTicks = 12

/** A setting's icon, title, note and badge, filling the row but for what follows them. */
@Composable
private fun RowScope.SettingLabel(icon: ImageVector, title: String, note: String?, badge: String?) {
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

/** An action in the panel: an icon and a label on a raised surface. */
@Composable
internal fun PanelButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(PanelColors.Surface)
            .border(1.dp, PanelColors.Border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = PanelColors.TextSecondary, modifier = Modifier.size(16.dp))
        Text(
            label,
            modifier = Modifier.padding(start = 6.dp),
            color = PanelColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
