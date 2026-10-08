package com.kappmaker.devicepreview.preview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatTextdirectionLToR
import androidx.compose.material.icons.automirrored.outlined.FormatTextdirectionRToL
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.StayCurrentLandscape
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kappmaker.devicepreview.core.Backdrop
import com.kappmaker.devicepreview.core.Device
import com.kappmaker.devicepreview.core.DevicePreset
import com.kappmaker.devicepreview.core.FontScaleSteps
import com.kappmaker.devicepreview.core.FreeformDevice
import com.kappmaker.devicepreview.core.SimulatedDevice
import com.kappmaker.devicepreview.core.adaptedToDevice
import com.kappmaker.devicepreview.core.currentContentSize
import com.kappmaker.devicepreview.core.currentLogicalSize
import com.kappmaker.devicepreview.core.displayScaleSteps
import com.kappmaker.devicepreview.core.isRotatable
import com.kappmaker.devicepreview.core.laidOutInPortrait
import com.kappmaker.devicepreview.core.nearest
import com.kappmaker.devicepreview.core.portraitScreenSize
import kotlin.math.roundToInt

private val OrientationOptions = listOf(
    SegmentOption("Portrait", Icons.Outlined.StayCurrentPortrait),
    SegmentOption("Landscape", Icons.Outlined.StayCurrentLandscape),
)

private val AppearanceOptions = listOf(
    SegmentOption("Light", Icons.Outlined.LightMode),
    SegmentOption("Dark", Icons.Outlined.DarkMode),
)

private val DirectionOptions = listOf(
    SegmentOption("Left to right", Icons.AutoMirrored.Outlined.FormatTextdirectionLToR),
    SegmentOption("Right to left", Icons.AutoMirrored.Outlined.FormatTextdirectionRToL),
)

// In the order of Backdrop's entries.
private val BackdropOptions = listOf(
    SegmentOption("Light", Icons.Outlined.LightMode),
    SegmentOption("Dark", Icons.Outlined.DarkMode),
    SegmentOption("Auto", Icons.Outlined.BrightnessAuto),
)

private const val NotAvailable = "Not available on this device"

/** E.g. "Galaxy S25 1080x2340.png", or "Galaxy S25 with frame.png". */
private fun screenshotName(device: Device, image: ImageBitmap, includesFrame: Boolean): String =
    if (includesFrame) "${device.name} with frame.png" else "${device.name} ${image.width}x${image.height}.png"

/**
 * The configuration panel: the device, and how it is shown.
 *
 * @param panelItems Your own items, shown in order below the panel's settings. Each is drawn as it
 * is given, as wide as the panel's settings, inside the panel's dark Material theme: how it looks
 * and what it does are up to you.
 */
@Composable
public fun ConfigurationView(
    modifier: Modifier = Modifier,
    simulatedDevice: SimulatedDevice,
    onChangeConfiguration: (SimulatedDevice) -> Unit,
    panelItems: List<@Composable () -> Unit> = emptyList(),
) {
    val configuration = simulatedDevice.configuration
    val device = simulatedDevice.device
    fun update(change: SimulatedDevice.() -> SimulatedDevice) = onChangeConfiguration(simulatedDevice.change())

    PanelTheme {
        var isPickerShown by remember { mutableStateOf(false) }
        if (isPickerShown) {
            DevicePickerDialog(
                selected = device,
                // Freeform starts as the device is now, turned as it is.
                freeform = device as? FreeformDevice ?: FreeformDevice(simulatedDevice.currentLogicalSize),
                onPick = {
                    update { copy(device = it).adaptedToDevice() }
                    isPickerShown = false
                },
                onDismiss = { isPickerShown = false },
            )
        }

        Column(
            modifier = modifier
                .background(PanelColors.Background)
                .drawBehind { drawLine(PanelColors.Border, Offset.Zero, Offset(0f, size.height), 1.dp.toPx()) }
        ) {
            PanelHeader()

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                DeviceCard(device, isPortrait = device.laidOutInPortrait(configuration.isPortrait)) { isPickerShown = true }

                Spacer(Modifier.height(24.dp))
                if (device is FreeformDevice) {
                    // A freeform window has no orientation: it is any shape, which is its size.
                    SectionLabel("Size")
                    SizeFields(device.size) { size -> update { copy(device = FreeformDevice(size)) } }
                } else {
                    SectionLabel("Orientation")
                    // A desktop window can't turn: it is locked to its natural, wide shape, which is landscape.
                    SegmentedControl(
                        options = OrientationOptions,
                        selectedIndex = if (device.isRotatable && configuration.isPortrait) 0 else 1,
                        onSelect = { index ->
                            update {
                                copy(configuration = configuration.copy(isPortrait = index == 0)).adaptedToDevice()
                            }
                        },
                        enabled = device.isRotatable,
                    )
                    if (!device.isRotatable) {
                        Text(
                            "Desktop windows are landscape only",
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                            color = PanelColors.TextTertiary,
                            fontSize = 12.sp,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                SectionLabel("Appearance")
                SegmentedControl(
                    options = AppearanceOptions,
                    selectedIndex = if (configuration.isDarkMode) 1 else 0,
                    onSelect = { index -> update { copy(configuration = configuration.copy(isDarkMode = index == 1)) } },
                )

                Spacer(Modifier.height(20.dp))
                SectionLabel("Text & display")
                val fontSteps = FontScaleSteps
                StepSlider(
                    icon = Icons.Outlined.FormatSize,
                    title = "Text size",
                    note = "Real device scaling options may vary",
                    labels = fontSteps.map { it.label },
                    selectedIndex = fontSteps.indexOf(fontSteps.nearest(configuration.fontScale)),
                    onSelect = { index -> update { copy(configuration = configuration.copy(fontScale = fontSteps[index].scale)) } },
                )
                val displaySteps = device.displayScaleSteps
                val contentSize = simulatedDevice.currentContentSize
                StepSlider(
                    icon = Icons.Outlined.ZoomIn,
                    title = "Display size",
                    note = "App sees ${contentSize.width.roundToInt()} × ${contentSize.height.roundToInt()} dp",
                    labels = displaySteps.map { it.label },
                    selectedIndex = displaySteps.indexOf(displaySteps.nearest(configuration.displayScale)),
                    onSelect = { index ->
                        update { copy(configuration = configuration.copy(displayScale = displaySteps[index].scale)) }
                    },
                )

                Spacer(Modifier.height(20.dp))
                SectionLabel("Layout direction")
                SegmentedControl(
                    options = DirectionOptions,
                    selectedIndex = if (configuration.layoutDirection == LayoutDirection.Rtl) 1 else 0,
                    onSelect = { index ->
                        val direction = if (index == 1) LayoutDirection.Rtl else LayoutDirection.Ltr
                        update { copy(configuration = configuration.copy(layoutDirection = direction)) }
                    },
                )

                Spacer(Modifier.height(20.dp))
                SectionLabel("Display")
                ToggleRow(
                    icon = Icons.Outlined.Smartphone,
                    title = "Device frame",
                    note = if (device is FreeformDevice) "Window border" else "Bezel and screen corners",
                    checked = configuration.isFrameVisible,
                    onCheckedChange = { update { copy(configuration = configuration.copy(isFrameVisible = it)) } },
                )
                val hasSystemUi = device.systemUi?.isEmpty == false
                ToggleRow(
                    icon = Icons.Outlined.SignalCellularAlt,
                    title = "System UI",
                    note = if (hasSystemUi) "Status bar and gesture bar" else NotAvailable,
                    checked = hasSystemUi && configuration.isSystemUiVisible,
                    enabled = hasSystemUi,
                    onCheckedChange = { update { copy(configuration = configuration.copy(isSystemUiVisible = it)) } },
                )
                val keyboardHeight = simulatedDevice.currentKeyboardHeight
                val isKeyboardShown = keyboardHeight != null && configuration.isKeyboardVisible
                ToggleRow(
                    icon = Icons.Outlined.Keyboard,
                    title = "Keyboard",
                    note = if (keyboardHeight == null) NotAvailable else "On-screen keyboard",
                    badge = keyboardHeight?.takeIf { isKeyboardShown }?.let { "${it.roundToInt()} dp" },
                    checked = isKeyboardShown,
                    enabled = keyboardHeight != null,
                    onCheckedChange = { update { copy(configuration = configuration.copy(isKeyboardVisible = it)) } },
                )

                Spacer(Modifier.height(20.dp))
                SectionLabel("Backdrop")
                SegmentedControl(
                    options = BackdropOptions,
                    selectedIndex = configuration.backdrop.ordinal,
                    onSelect = { index -> update { copy(configuration = configuration.copy(backdrop = Backdrop.entries[index])) } },
                )
                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    icon = Icons.Outlined.GridOn,
                    title = "Dot grid",
                    note = "Dots behind the device",
                    checked = configuration.isDotGridVisible,
                    onCheckedChange = { update { copy(configuration = configuration.copy(isDotGridVisible = it)) } },
                )

                val screenshots = LocalDeviceScreenshots.current
                val download = pngDownloader
                if (screenshots != null && download != null) {
                    Spacer(Modifier.height(20.dp))
                    SectionLabel("Screenshot")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (includeFrame in listOf(false, true)) {
                            PanelButton(
                                icon = if (includeFrame) Icons.Outlined.PhoneAndroid else Icons.Outlined.Screenshot,
                                label = if (includeFrame) "With frame" else "Screen",
                                enabled = screenshots.canCapture,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    screenshots.capture(includeFrame)?.let { image ->
                                        download(image, screenshotName(device, image, includeFrame))
                                    }
                                },
                            )
                        }
                    }
                    Text(
                        "A PNG at the device's own resolution",
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                        color = PanelColors.TextTertiary,
                        fontSize = 12.sp,
                    )
                }

                if (panelItems.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    panelItems.forEach { item -> item() }
                }
            }

            HorizontalDivider(color = PanelColors.Border)
            ToggleRow(
                icon = Icons.Outlined.Visibility,
                title = "Preview mode",
                note = "Off runs the app full screen",
                checked = configuration.isPreviewModeEnabled,
                onCheckedChange = { update { copy(configuration = configuration.copy(isPreviewModeEnabled = it)) } },
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
private fun PanelHeader() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .background(Brush.linearGradient(listOf(PanelColors.Accent, Color(0xFF4FA3FF))), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Devices, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Text(
            "Device Preview",
            modifier = Modifier.padding(start = 12.dp),
            color = PanelColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** The current device, which opens the picker. Its thumbnail turns with the device. */
@Composable
private fun DeviceCard(device: Device, isPortrait: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val turn by animateFloatAsState(if (isPortrait) 0f else -90f, label = "thumbnailTurn")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PanelColors.SurfaceRaised, PanelColors.Surface)))
            .border(1.dp, PanelColors.Border, shape)
            .clickable(role = Role.Button, onClickLabel = "Change device", onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(56.dp).background(PanelColors.Background, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            DeviceThumbnail(device, Modifier.size(44.dp).graphicsLayer { rotationZ = turn }, isHighlighted = true)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            val preset = device as? DevicePreset
            val origin = if (device is FreeformDevice) {
                "Drag a corner to resize"
            } else {
                listOfNotNull(preset?.brand, preset?.year?.toString())
                    .ifEmpty { listOf(device.type.displayName) }
                    .joinToString(" · ")
            }
            val size = device.portraitScreenSize
            // A freeform window is measured in dp alone: it has no pixel ratio of its own.
            val dimensions = "${size.width.roundToInt()} × ${size.height.roundToInt()}"
            Text(device.name, color = PanelColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(origin, color = PanelColors.TextSecondary, fontSize = 12.sp)
            Text(
                if (device is FreeformDevice) dimensions else "$dimensions · ${formatPixelRatio(device.screenSize.density)}",
                color = PanelColors.TextTertiary,
                fontSize = 12.sp,
            )
        }
        Icon(Icons.Outlined.UnfoldMore, contentDescription = null, tint = PanelColors.TextSecondary, modifier = Modifier.size(20.dp))
    }
}

/** E.g. "3x" or "2.625x". */
internal fun formatPixelRatio(ratio: Float): String {
    val thousandths = (ratio * 1000).roundToInt()
    val fraction = thousandths % 1000
    return if (fraction == 0) {
        "${thousandths / 1000}x"
    } else {
        "${thousandths / 1000}.${fraction.toString().padStart(3, '0').trimEnd('0')}x"
    }
}

/** The width and height of a freeform window, to type in, with a button that swaps them. */
@Composable
private fun SizeFields(size: Size, onChange: (Size) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SizeField("Width", size.width, Modifier.weight(1f)) { onChange(Size(it, size.height)) }
        Text(
            "×",
            modifier = Modifier.padding(horizontal = 8.dp),
            color = PanelColors.TextTertiary,
            fontSize = 14.sp,
        )
        SizeField("Height", size.height, Modifier.weight(1f)) { onChange(Size(size.width, it)) }
        IconButton(
            onClick = { onChange(Size(size.height, size.width)) },
            modifier = Modifier.padding(start = 4.dp).size(36.dp).pointerHoverIcon(PointerIcon.Hand),
        ) {
            Icon(Icons.Outlined.SwapHoriz, contentDescription = "Swap width and height", tint = PanelColors.TextSecondary)
        }
    }
    Text(
        "$MinFreeformSizeDp–$MaxFreeformSizeDp dp, to the whole dp",
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        color = PanelColors.TextTertiary,
        fontSize = 12.sp,
    )
}

private val MinFreeformSizeDp = MinFreeformSize.roundToInt()
private val MaxFreeformSizeDp = MaxFreeformSize.roundToInt()

/**
 * A side of a freeform window, in dp. What is typed is applied on Enter or when the field loses
 * focus, kept between [MinFreeformSize] and [MaxFreeformSize]; what isn't a number is dropped.
 */
@Composable
private fun SizeField(label: String, value: Float, modifier: Modifier = Modifier, onCommit: (Float) -> Unit) {
    val shown = value.roundToInt().toString()
    // Starts over whenever the size changes from elsewhere, such as by dragging a corner.
    var text by remember(shown) { mutableStateOf(shown) }
    var isFocused by remember { mutableStateOf(false) }
    fun commit() {
        val typed = text.toIntOrNull()
        if (typed == null) {
            text = shown
            return
        }
        val size = typed.coerceIn(MinFreeformSizeDp, MaxFreeformSizeDp)
        text = size.toString()
        if (size != value.roundToInt()) onCommit(size.toFloat())
    }
    val shape = RoundedCornerShape(10.dp)
    BasicTextField(
        value = text,
        onValueChange = { text = it.filter(Char::isDigit).take(MaxFreeformSizeDp.toString().length) },
        modifier = modifier
            .onFocusChanged {
                if (isFocused && !it.isFocused) commit()
                isFocused = it.isFocused
            }
            .onPreviewKeyEvent { event ->
                val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                if (isEnter && event.type == KeyEventType.KeyDown) commit()
                isEnter
            },
        singleLine = true,
        textStyle = TextStyle(color = PanelColors.TextPrimary, fontSize = 14.sp),
        cursorBrush = SolidColor(PanelColors.Accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        decorationBox = { field ->
            Column(
                Modifier
                    .height(48.dp)
                    .background(PanelColors.Surface, shape)
                    .border(1.dp, if (isFocused) PanelColors.Accent else PanelColors.Border, shape)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(label.uppercase(), color = PanelColors.TextTertiary, fontSize = 9.sp, letterSpacing = 0.8.sp)
                field()
            }
        },
    )
}
