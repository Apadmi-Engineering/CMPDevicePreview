package com.apadmi.cmpdevicepreview.preview

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
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.apadmi.cmpdevicepreview.core.Backdrop
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.DevicePreset
import com.apadmi.cmpdevicepreview.core.FontScaleSteps
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.core.currentContentSize
import com.apadmi.cmpdevicepreview.core.displayScaleSteps
import com.apadmi.cmpdevicepreview.core.hasFrameArtwork
import com.apadmi.cmpdevicepreview.core.isLaidOutInPortrait
import com.apadmi.cmpdevicepreview.core.isRotatable
import com.apadmi.cmpdevicepreview.core.nearest

import kotlin.math.roundToInt

private const val NotAvailable = "Not available for this frame"

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

private val BackdropOptions = Backdrop.entries.map { backdrop ->
    when (backdrop) {
        Backdrop.LIGHT -> SegmentOption("Light", Icons.Outlined.LightMode)
        Backdrop.DARK -> SegmentOption("Dark", Icons.Outlined.DarkMode)
        Backdrop.AUTO -> SegmentOption("Auto", Icons.Outlined.BrightnessAuto)
    }
}

private val MinFreeformSizeDp = MinFreeformSize.roundToInt()
private val MaxFreeformSizeDp = MaxFreeformSize.roundToInt()

/**
 * The configuration panel, which picks the device and sets how it and the preview are shown.
 *
 * @param simulatedDevice The device and configuration the panel shows the settings of.
 * @param onSimulatedDeviceChange Called with the new device and configuration when the picker or a
 * setting changes them, fitted to the device. Pass it back as [simulatedDevice] to apply it.
 * @param previewSettings The settings of the preview around the device that the panel shows.
 * @param onPreviewSettingsChange Called with the new settings when the backdrop, the dot grid or
 * preview mode is changed. Pass them back as [previewSettings] to apply them.
 * @param modifier The modifier for the panel.
 * @param panelItems Your own items, shown in order below the panel's settings, as wide as them and
 * inside the panel's dark Material theme.
 */
@Composable
public fun ConfigurationView(
    simulatedDevice: SimulatedDevice,
    onSimulatedDeviceChange: (SimulatedDevice) -> Unit,
    previewSettings: PreviewSettings,
    onPreviewSettingsChange: (PreviewSettings) -> Unit,
    modifier: Modifier = Modifier,
    panelItems: List<@Composable () -> Unit> = emptyList(),
) {
    val device = simulatedDevice.device
    // Every change is fitted to the device, whichever setting it comes from.
    val update = { changed: SimulatedDevice -> onSimulatedDeviceChange(changed.adaptedToDevice()) }
    val updateConfiguration = { changed: DeviceConfiguration -> update(simulatedDevice.copy(configuration = changed)) }

    PanelTheme {
        var isPickerShown by remember { mutableStateOf(false) }
        if (isPickerShown) {
            DevicePickerDialog(
                selected = device,
                // Freeform starts as the device is now, turned as it is.
                freeform = device as? FreeformDevice ?: FreeformDevice(simulatedDevice.currentSize),
                onPick = {
                    update(simulatedDevice.copy(device = it))
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
                FrameSection(simulatedDevice, onChangeFrame = { isPickerShown = true })
                Spacer(Modifier.height(24.dp))
                if (device is FreeformDevice) {
                    SizeSection(device.size) { size -> update(simulatedDevice.copy(device = FreeformDevice(size))) }
                } else {
                    OrientationSection(device, simulatedDevice.configuration, updateConfiguration)
                }
                Spacer(Modifier.height(20.dp))
                AppearanceSection(simulatedDevice.configuration, updateConfiguration)
                Spacer(Modifier.height(20.dp))
                TextAndDisplaySection(simulatedDevice, updateConfiguration)
                Spacer(Modifier.height(20.dp))
                LayoutDirectionSection(simulatedDevice.configuration, updateConfiguration)
                Spacer(Modifier.height(20.dp))
                DisplaySection(simulatedDevice, updateConfiguration)
                Spacer(Modifier.height(20.dp))
                BackdropSection(previewSettings, onPreviewSettingsChange)
                ScreenshotSection(device)

                if (panelItems.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    panelItems.forEach { item -> item() }
                }
            }

            HorizontalDivider(color = PanelColors.Border)
            ToggleRow(
                icon = Icons.Outlined.Visibility,
                title = "Preview mode",
                note = "Off shows just the app, with a button to turn it back on",
                checked = previewSettings.isPreviewModeEnabled,
                onCheckedChange = { onPreviewSettingsChange(previewSettings.copy(isPreviewModeEnabled = it)) },
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
        DevicePreviewLogo()
        Text(
            "Device Preview",
            modifier = Modifier.padding(start = 12.dp),
            color = PanelColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FrameSection(simulatedDevice: SimulatedDevice, onChangeFrame: () -> Unit) {
    val device = simulatedDevice.device
    SectionLabel("Frame")
    DeviceCard(device, isPortrait = device.isLaidOutInPortrait(simulatedDevice.configuration.isPortrait), onChangeFrame)
    FrameDisclaimer()
}

@Composable
private fun SizeSection(size: Size, onChange: (Size) -> Unit) {
    SectionLabel("Size")
    SizeFields(size, onChange)
}

@Composable
private fun OrientationSection(
    device: Device,
    configuration: DeviceConfiguration,
    onChange: (DeviceConfiguration) -> Unit
) {
    SectionLabel("Orientation")
    // A desktop window can't turn, so it shows as landscape.
    SegmentedControl(
        options = OrientationOptions,
        selectedIndex = if (device.isRotatable && configuration.isPortrait) 0 else 1,
        onSelect = { index -> onChange(configuration.copy(isPortrait = index == 0)) },
        enabled = device.isRotatable,
    )
    if (!device.isRotatable) {
        PanelCaption("Desktop windows are landscape only")
    }
}

@Composable
private fun AppearanceSection(configuration: DeviceConfiguration, onChange: (DeviceConfiguration) -> Unit) {
    SectionLabel("Appearance")
    SegmentedControl(
        options = AppearanceOptions,
        selectedIndex = if (configuration.isDarkMode) 1 else 0,
        onSelect = { index -> onChange(configuration.copy(isDarkMode = index == 1)) },
    )
}

@Composable
private fun TextAndDisplaySection(simulatedDevice: SimulatedDevice, onChange: (DeviceConfiguration) -> Unit) {
    val configuration = simulatedDevice.configuration
    SectionLabel("Text & display")
    StepSlider(
        icon = Icons.Outlined.FormatSize,
        title = "Text size",
        note = "Real device scaling options may vary",
        labels = FontScaleSteps.map { it.label },
        selectedIndex = FontScaleSteps.indexOf(FontScaleSteps.nearest(configuration.fontScale)),
        onSelect = { index -> onChange(configuration.copy(fontScale = FontScaleSteps[index].scale)) },
    )
    val displaySteps = simulatedDevice.device.displayScaleSteps
    StepSlider(
        icon = Icons.Outlined.ZoomIn,
        title = "Display size",
        note = "App sees ${simulatedDevice.currentContentSize.dimensionsLabel()} dp",
        labels = displaySteps.map { it.label },
        selectedIndex = displaySteps.indexOf(displaySteps.nearest(configuration.displayScale)),
        onSelect = { index -> onChange(configuration.copy(displayScale = displaySteps[index].scale)) },
    )
}

@Composable
private fun LayoutDirectionSection(configuration: DeviceConfiguration, onChange: (DeviceConfiguration) -> Unit) {
    SectionLabel("Layout direction")
    val direction = configuration.layoutDirection ?: LocalLayoutDirection.current
    SegmentedControl(
        options = DirectionOptions,
        selectedIndex = if (direction == LayoutDirection.Rtl) 1 else 0,
        onSelect = { index ->
            onChange(configuration.copy(layoutDirection = if (index == 1) LayoutDirection.Rtl else LayoutDirection.Ltr))
        },
    )
}

@Composable
private fun DisplaySection(simulatedDevice: SimulatedDevice, onChange: (DeviceConfiguration) -> Unit) {
    val device = simulatedDevice.device
    val configuration = simulatedDevice.configuration
    SectionLabel("Display")
    ToggleRow(
        icon = Icons.Outlined.Smartphone,
        title = if (device.hasFrameArtwork) "Bezel" else "Border",
        note = if (device.hasFrameArtwork) "Body and screen corners" else "Window border and shadow",
        checked = configuration.isFrameVisible,
        onCheckedChange = { onChange(configuration.copy(isFrameVisible = it)) },
    )
    val hasSystemUi = device.systemUi?.isEmpty == false
    ToggleRow(
        icon = Icons.Outlined.SignalCellularAlt,
        title = "System UI",
        note = if (hasSystemUi) "A static status bar and gesture bar" else NotAvailable,
        checked = hasSystemUi && configuration.isSystemUiVisible,
        enabled = hasSystemUi,
        onCheckedChange = { onChange(configuration.copy(isSystemUiVisible = it)) },
    )
    val keyboardHeight = simulatedDevice.currentKeyboardHeight
    val isKeyboardShown = keyboardHeight != null && configuration.isKeyboardVisible
    ToggleRow(
        icon = Icons.Outlined.Keyboard,
        title = "Keyboard",
        note = if (keyboardHeight == null) NotAvailable else "The space a keyboard covers",
        badge = keyboardHeight?.takeIf { isKeyboardShown }?.let { "${it.roundToInt()} dp" },
        checked = isKeyboardShown,
        enabled = keyboardHeight != null,
        onCheckedChange = { onChange(configuration.copy(isKeyboardVisible = it)) },
    )
}

@Composable
private fun BackdropSection(previewSettings: PreviewSettings, onChange: (PreviewSettings) -> Unit) {
    SectionLabel("Backdrop")
    SegmentedControl(
        options = BackdropOptions,
        selectedIndex = previewSettings.backdrop.ordinal,
        onSelect = { index -> onChange(previewSettings.copy(backdrop = Backdrop.entries[index])) },
    )
    Spacer(Modifier.height(8.dp))
    ToggleRow(
        icon = Icons.Outlined.GridOn,
        title = "Dot grid",
        note = "Dots behind the frame",
        checked = previewSettings.isDotGridVisible,
        onCheckedChange = { onChange(previewSettings.copy(isDotGridVisible = it)) },
    )
}

/** Buttons that save the screen, or the whole device, as a PNG, where the platform can. */
@Composable
private fun ScreenshotSection(device: Device) {
    val screenshots = LocalDeviceScreenshots.current
    val download = pngDownloader
    if (screenshots == null || download == null) {
        return
    }
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
    PanelCaption("PNGs at the device's own resolution")
}

/** The current frame, which opens the picker. Its thumbnail turns with the frame. */
@Composable
private fun DeviceCard(
    device: Device,
    isPortrait: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val turn by animateFloatAsState(if (isPortrait) 0f else -90f, label = "thumbnailTurn")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PanelColors.SurfaceRaised, PanelColors.Surface)))
            .border(1.dp, PanelColors.Border, shape)
            .clickable(role = Role.Button, onClickLabel = "Change frame", onClick = onClick)
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
            val dimensions = device.portraitSize.dimensionsLabel()
            Text(device.name, color = PanelColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(origin, color = PanelColors.TextSecondary, fontSize = 12.sp)
            Text(
                device.devicePixelRatio?.let { "$dimensions · ${formatPixelRatio(it)}" } ?: dimensions,
                color = PanelColors.TextTertiary,
                fontSize = 12.sp,
            )
        }
        Icon(Icons.Outlined.UnfoldMore, contentDescription = null, tint = PanelColors.TextSecondary, modifier = Modifier.size(20.dp))
    }
}

/** A note that a frame shows a device's size and shape, but doesn't simulate the device. */
@Composable
private fun FrameDisclaimer() {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 10.dp)) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = PanelColors.TextTertiary,
            modifier = Modifier.padding(top = 1.dp).size(14.dp),
        )
        Text(
            "Frames give a feel for a device's size and shape. They don't simulate the device: your " +
                    "app runs here, in a window that size.",
            modifier = Modifier.padding(start = 6.dp),
            color = PanelColors.TextTertiary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
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
    PanelCaption("$MinFreeformSizeDp–$MaxFreeformSizeDp dp")
}

/**
 * A side of a freeform window, in dp. What is typed is applied on Enter or when the field loses
 * focus, kept between [MinFreeformSize] and [MaxFreeformSize]; what isn't a number is dropped.
 */
@Composable
private fun SizeField(
    label: String,
    value: Float,
    modifier: Modifier = Modifier,
    onCommit: (Float) -> Unit
) {
    val shown = value.roundToInt().toString()
    // Starts over whenever the size changes from elsewhere, such as by dragging a corner.
    var text by remember(shown) { mutableStateOf(shown) }
    var isFocused by remember { mutableStateOf(false) }
    val commit: () -> Unit = {
        val typed = text.toIntOrNull()
        if (typed == null) {
            text = shown
        } else {
            val size = typed.coerceIn(MinFreeformSizeDp, MaxFreeformSizeDp)
            text = size.toString()
            if (size != value.roundToInt()) {
                onCommit(size.toFloat())
            }
        }
    }
    val shape = RoundedCornerShape(10.dp)
    BasicTextField(
        value = text,
        onValueChange = { text = it.filter(Char::isDigit).take(MaxFreeformSizeDp.toString().length) },
        modifier = modifier
            .onFocusChanged {
                if (isFocused && !it.isFocused) {
                    commit()
                }
                isFocused = it.isFocused
            }
            .onPreviewKeyEvent { event ->
                val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                if (isEnter && event.type == KeyEventType.KeyDown) {
                    commit()
                }
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
