package com.kappmaker.devicepreview.preview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.StayCurrentLandscape
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kappmaker.devicepreview.core.Device
import com.kappmaker.devicepreview.core.DevicePreset
import com.kappmaker.devicepreview.core.SimulatedDevice
import com.kappmaker.devicepreview.core.isRotatable
import com.kappmaker.devicepreview.core.laidOutInPortrait
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

private const val NotAvailable = "Not available on this device"

@Composable
public fun ConfigurationView(
    modifier: Modifier = Modifier,
    simulatedDevice: SimulatedDevice,
    onChangeConfiguration: (SimulatedDevice) -> Unit
) {
    val configuration = simulatedDevice.configuration
    val device = simulatedDevice.device
    fun update(change: SimulatedDevice.() -> SimulatedDevice) = onChangeConfiguration(simulatedDevice.change())

    PanelTheme {
        var isPickerShown by remember { mutableStateOf(false) }
        if (isPickerShown) {
            DevicePickerDialog(
                selected = device,
                onPick = {
                    update { copy(device = it).droppingUnavailableKeyboard() }
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
                SectionLabel("Orientation")
                // A desktop window can't turn: it is locked to its natural, wide shape, which is landscape.
                SegmentedControl(
                    options = OrientationOptions,
                    selectedIndex = if (device.isRotatable && configuration.isPortrait) 0 else 1,
                    onSelect = { index ->
                        update {
                            copy(configuration = configuration.copy(isPortrait = index == 0)).droppingUnavailableKeyboard()
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

                Spacer(Modifier.height(20.dp))
                SectionLabel("Appearance")
                SegmentedControl(
                    options = AppearanceOptions,
                    selectedIndex = if (configuration.isDarkMode) 1 else 0,
                    onSelect = { index -> update { copy(configuration = configuration.copy(isDarkMode = index == 1)) } },
                )

                Spacer(Modifier.height(20.dp))
                SectionLabel("Display")
                ToggleRow(
                    icon = Icons.Outlined.Smartphone,
                    title = "Device frame",
                    note = "Bezel and screen corners",
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
            val origin = listOfNotNull(preset?.brand, preset?.year?.toString())
                .ifEmpty { listOf(device.type.displayName) }
                .joinToString(" · ")
            val size = device.portraitScreenSize
            Text(device.name, color = PanelColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(origin, color = PanelColors.TextSecondary, fontSize = 12.sp)
            Text(
                "${size.width.roundToInt()} × ${size.height.roundToInt()} · ${formatPixelRatio(device.screenSize.density)}",
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

/**
 * Keeps a raised keyboard raised across a device or orientation change, at the new height, unless
 * the device has no keyboard there.
 */
private fun SimulatedDevice.droppingUnavailableKeyboard(): SimulatedDevice =
    if (configuration.isKeyboardVisible && currentKeyboardHeight == null) {
        copy(configuration = configuration.copy(isKeyboardVisible = false))
    } else {
        this
    }
