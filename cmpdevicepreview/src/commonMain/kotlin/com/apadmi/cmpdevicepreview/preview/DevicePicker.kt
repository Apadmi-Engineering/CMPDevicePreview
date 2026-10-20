package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceKind
import com.apadmi.cmpdevicepreview.core.DevicePreset
import com.apadmi.cmpdevicepreview.core.DeviceType
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import com.apadmi.cmpdevicepreview.frame.DeviceFramePainter
import com.apadmi.cmpdevicepreview.isMobilePlatform
import kotlin.math.min

/** The words a search has to find in the pinned Freeform row. */
private const val FreeformKeywords = "freeform custom resize window"

/** How many rows the list shows above the selected device when it opens. */
private const val RowsAboveSelected = 2

/** The device kinds the picker can narrow its list to, with null for every device. */
private val KindFilters: List<Pair<String, DeviceKind?>> = listOf(
    "All" to null,
    "Phones" to DeviceKind.PHONE,
    "Tablets" to DeviceKind.TABLET,
    "Foldables" to DeviceKind.FOLDABLE,
    "Desktop" to DeviceKind.DESKTOP,
)

/** The devices offered by the configuration view: by platform, then brand, newest first. */
internal val allDevices: List<DevicePreset> = DevicePresets.all.sortedWith(
    compareBy<DevicePreset> { it.type }
        .thenBy { it.brand.orEmpty() }
        .thenByDescending { it.year ?: 0 }
        .thenBy { it.name }
)

/** The built-in devices by platform, in the order the picker lists them. */
private val deviceGroups: Map<DeviceType, List<DevicePreset>> = allDevices.groupBy { it.type }

/** Whether [query] finds this preset, among those of [kind] when it is set. */
private fun DevicePreset.isFoundBy(query: String, kind: DeviceKind?): Boolean {
    if (kind != null && this.kind != kind) {
        return false
    }
    return query.hasAllWordsIn(listOfNotNull(name, brand, type.displayName).joinToString(" "))
}

/** Whether every word of the query, if any, appears in [haystack]. */
private fun String.hasAllWordsIn(haystack: String): Boolean {
    val text = haystack.lowercase()
    return lowercase().split(' ').filter { it.isNotBlank() }.all { it in text }
}

/**
 * A searchable list of the built-in devices, grouped by platform, with [selected] checked. The
 * Freeform row comes first, and picking it gives [freeform].
 *
 * @param selected The device that is checked, and in view when the list opens.
 * @param freeform The window that picking the Freeform row gives.
 * @param onPick Called with the device that was picked: a row, or the highlighted match when a
 * search is submitted.
 * @param onDismiss Called when the dialog is closed, by its close button or by dismissing it.
 */
@Composable
internal fun DevicePickerDialog(
    selected: Device,
    freeform: FreeformDevice,
    onPick: (Device) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        PanelTheme {
            var query by remember { mutableStateOf("") }
            var kind by remember { mutableStateOf<DeviceKind?>(null) }
            val results = remember(query, kind) { allDevices.filter { it.isFoundBy(query, kind) } }
            val groups = results.groupBy { it.type }
            // Freeform isn't a kind of device, so only the unfiltered list has it.
            val showFreeform = kind == null && query.hasAllWordsIn(FreeformKeywords)
            // Opens with the current device in view.
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = remember {
                    val position = positionInList(selected, deviceGroups, hasFreeform = true) ?: 0
                    (position - RowsAboveSelected).coerceAtLeast(0)
                }
            )
            // A search or filter shows its matches from the top.
            LaunchedEffect(query, kind) {
                if (query.isNotEmpty() || kind != null) {
                    listState.scrollToItem(0)
                }
            }
            val rowsInOrder: List<Device> = buildList {
                if (showFreeform) {
                    add(freeform)
                }
                groups.values.forEach(::addAll)
            }
            var activeIndex by remember(query, kind) { mutableStateOf<Int?>(if (query.isBlank()) null else 0) }
            val active = activeIndex?.let(rowsInOrder::getOrNull)
            LaunchedEffect(activeIndex) {
                val position = active?.let { positionInList(it, groups, showFreeform) } ?: return@LaunchedEffect
                val inView = listState.layoutInfo.visibleItemsInfo
                if (inView.isEmpty() || position <= inView.first().index + 1 || position >= inView.last().index) {
                    listState.animateScrollToItem((position - RowsAboveSelected).coerceAtLeast(0))
                }
            }

            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 680.dp)
                    .fillMaxHeight(0.86f)
                    .shadow(32.dp, shape)
                    .clip(shape)
                    .background(PanelColors.Background)
                    .border(1.dp, PanelColors.Border, shape)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Choose a frame", color = PanelColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("${allDevices.size} frames", color = PanelColors.TextTertiary, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = PanelColors.TextSecondary)
                    }
                }
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onSubmit = { active?.let(onPick) },
                    onMove = { step -> activeIndex = ((activeIndex ?: -1) + step).coerceIn(0, maxOf(rowsInOrder.lastIndex, 0)) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for ((label, filter) in KindFilters) {
                        FilterChip(label, selected = kind == filter) { kind = filter }
                    }
                }
                HorizontalDivider(color = PanelColors.Border)

                if (results.isEmpty() && !showFreeform) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No frames match \"$query\"", color = PanelColors.TextTertiary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    ) {
                        if (showFreeform) {
                            item(key = "freeform") {
                                DeviceRow(
                                    device = freeform,
                                    details = "Any size · drag a corner to resize",
                                    isSelected = selected is FreeformDevice,
                                    isActive = active == freeform,
                                ) { onPick(freeform) }
                            }
                        }
                        for ((type, devices) in groups) {
                            stickyHeader(key = type.name) {
                                Text(
                                    "${type.displayName.uppercase()}  ·  ${devices.size}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(PanelColors.Background)
                                        .padding(start = 12.dp, top = 14.dp, bottom = 6.dp),
                                    color = PanelColors.TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.9.sp,
                                )
                            }
                            items(devices, key = { it.id }) { device ->
                                DeviceRow(
                                    device,
                                    deviceDetails(device),
                                    isSelected = device == selected,
                                    isActive = device == active,
                                ) { onPick(device) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The device's own frame in miniature, fitted inside the node, with its screen lit by a gradient;
 * a plain icon for a device without artwork.
 *
 * @param device The device to draw.
 * @param modifier The modifier for the thumbnail, which also sets its size.
 * @param isHighlighted Whether the thumbnail is drawn in the accent colour, as for the selected
 * device, rather than muted.
 */
@Composable
internal fun DeviceThumbnail(
    device: Device,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val frame = device.frame?.takeUnless { it.isEmpty }
    val painter = remember(frame) { frame?.let(::DeviceFramePainter) }
    if (frame == null || painter?.body == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(
                deviceIcon(device),
                contentDescription = null,
                tint = if (isHighlighted) PanelColors.AccentBright else PanelColors.TextSecondary,
                modifier = Modifier.fillMaxSize(0.6f),
            )
        }
        return
    }
    val screen = device.portraitSize
    val bounds = remember(frame, screen) { frame.bodyBounds(screen, isPortrait = true) }
    val screenOutline = remember(painter, screen) {
        painter.screenClip(screen, isPortrait = true) ?: Path().apply { addRect(Rect(Offset.Zero, screen)) }
    }
    val screenColors = if (isHighlighted) {
        listOf(PanelColors.Accent, PanelColors.AccentBlue)
    } else {
        listOf(PanelColors.ThumbnailScreenTop, PanelColors.ThumbnailScreenBottom)
    }
    // The artwork's bezels are near-black, which vanishes on the panel: draw the body as a
    // silhouette instead, keeping its outline, buttons and camera cut-outs.
    val bodyPaint = remember { Paint().apply { colorFilter = ColorFilter.tint(PanelColors.ThumbnailBody) } }
    Canvas(modifier) {
        val scale = min(size.width / bounds.width, size.height / bounds.height)
        withTransform({
            translate((size.width - bounds.width * scale) / 2f, (size.height - bounds.height * scale) / 2f)
            scale(scale, scale, pivot = Offset.Zero)
            translate(-bounds.left, -bounds.top)
        }) {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(bounds, bodyPaint)
                painter.paintBody(canvas, screen, isPortrait = true)
                canvas.restore()
            }
            drawPath(screenOutline, Brush.linearGradient(screenColors, start = Offset.Zero, end = Offset(screen.width, screen.height)))
        }
    }
}

// The list is Freeform, if shown, then a header before each platform's devices, so that is what comes before one.
private fun positionInList(
    device: Device,
    groups: Map<DeviceType, List<DevicePreset>>,
    hasFreeform: Boolean
): Int? {
    if (device is FreeformDevice) {
        return 0
    }
    val devicesBefore = groups.values.flatten().indexOf(device)
    if (devicesBefore < 0) {
        return null
    }
    val headersUpToIt = groups.keys.indexOf(device.type) + 1
    return (if (hasFreeform) 1 else 0) + headersUpToIt + devicesBefore
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onMove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    // On a desktop or in a browser, typing filters straight away; on a phone that would raise the
    // keyboard over the list.
    LaunchedEffect(Unit) {
        if (!isMobilePlatform) {
            focusRequester.requestFocus()
        }
    }
    val shape = RoundedCornerShape(10.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                val step = when (event.key) {
                    Key.DirectionDown -> 1
                    Key.DirectionUp -> -1
                    else -> 0
                }
                if (step != 0 && event.type == KeyEventType.KeyDown) {
                    onMove(step)
                }
                step != 0
            },
        singleLine = true,
        textStyle = TextStyle(color = PanelColors.TextPrimary, fontSize = 14.sp),
        cursorBrush = SolidColor(PanelColors.Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        decorationBox = { field ->
            Row(
                Modifier
                    .height(40.dp)
                    .background(PanelColors.Surface, shape)
                    .border(1.dp, PanelColors.Border, shape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = PanelColors.TextTertiary, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f).padding(start = 10.dp)) {
                    if (query.isEmpty()) {
                        Text("Search by name or brand", color = PanelColors.TextTertiary, fontSize = 14.sp)
                    }
                    field()
                }
            }
        },
    )
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        label,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) PanelColors.AccentSoft else PanelColors.Surface)
            .border(1.dp, if (selected) PanelColors.Accent.copy(alpha = 0.5f) else PanelColors.Border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = if (selected) PanelColors.AccentBright else PanelColors.TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun DeviceRow(
    device: Device,
    details: String,
    isSelected: Boolean,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) PanelColors.AccentSoft else Color.Transparent)
            .border(1.dp, if (isActive) PanelColors.Accent.copy(alpha = 0.5f) else Color.Transparent, shape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeviceThumbnail(device, Modifier.size(40.dp), isHighlighted = isSelected)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(device.name, color = PanelColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(details, color = PanelColors.TextTertiary, fontSize = 12.sp)
        }
        if (isSelected) {
            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = PanelColors.AccentBright, modifier = Modifier.size(18.dp))
        }
    }
}

private fun deviceIcon(device: Device): ImageVector {
    if (device is FreeformDevice) {
        return Icons.Outlined.AspectRatio
    }
    val isApple = device.type == DeviceType.IOS
    return when (device.kind) {
        DeviceKind.TABLET -> if (isApple) Icons.Filled.TabletMac else Icons.Filled.TabletAndroid
        DeviceKind.DESKTOP -> Icons.Filled.Computer
        DeviceKind.PHONE, DeviceKind.FOLDABLE -> when (device.type) {
            DeviceType.ANDROID -> Icons.Filled.PhoneAndroid
            DeviceType.IOS -> Icons.Filled.PhoneIphone
            DeviceType.OTHER -> Icons.Filled.DeviceHub
        }
    }
}

/** E.g. "Apple · 2024 · 402 × 874": brand, release year and logical portrait size. */
private fun deviceDetails(device: Device): String {
    val preset = device as? DevicePreset
    return listOfNotNull(preset?.brand, preset?.year?.toString(), device.portraitSize.dimensionsLabel()).joinToString(" · ")
}
