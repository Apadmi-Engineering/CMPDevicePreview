package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apadmi.cmpdevicepreview.core.Device
import kotlin.math.roundToInt

private const val ZoomStep = 1.25f
private const val RestingAlpha = 0.6f
private const val HoveredAlpha = 0.9f

private val ButtonSize = 28.dp

@Composable
internal fun PreviewToolbar(
    viewport: PreviewViewport,
    device: Device,
    isBackdropDark: Boolean,
    modifier: Modifier = Modifier
) {
    val ink = if (isBackdropDark) Color.White else Color.Black
    val resting = ink.copy(alpha = RestingAlpha)
    val hovered = ink.copy(alpha = HoveredAlpha)
    val hostDensity = LocalDensity.current.density
    // The zoom at which a dp of the device is a dp of this window.
    val actualSize = hostDensity / viewport.fitScale
    val step = { factor: Float ->
        val target = viewport.zoom * factor
        val isPassingActualSize = (viewport.zoom - actualSize) * (target - actualSize) < 0f
        viewport.zoomTo(if (isPassingActualSize) actualSize else target)
    }
    PanelTheme {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            PanelIconButton(
                Icons.Outlined.Remove, "Zoom out", "Zoom out", { step(1f / ZoomStep) },
                size = ButtonSize, tint = resting, hoverTint = hovered,
            )
            PanelTooltip("Actual size") {
                Text(
                    "${(viewport.zoom * viewport.fitScale / hostDensity * 100f).roundToInt()}%",
                    modifier = Modifier
                        .widthIn(min = 40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button) { viewport.zoomTo(actualSize) }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    color = resting,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
            PanelIconButton(
                Icons.Outlined.Add, "Zoom in", "Zoom in", { step(ZoomStep) },
                size = ButtonSize, tint = resting, hoverTint = hovered,
            )
            PanelIconButton(
                Icons.Outlined.FitScreen, "Fit to window", "Fit to window", viewport::reset,
                size = ButtonSize, tint = resting, hoverTint = hovered, isShown = viewport.isTransformed,
            )
            ScreenshotButtons(device, resting, hovered)
        }
    }
}

@Composable
private fun ScreenshotButtons(
    device: Device,
    tint: Color,
    hoverTint: Color
) {
    val screenshots = LocalDeviceScreenshots.current
    val download = pngDownloader
    if (screenshots == null || download == null) {
        return
    }
    Spacer(Modifier.width(8.dp))
    for (includeFrame in listOf(false, true)) {
        PanelIconButton(
            icon = if (includeFrame) Icons.Outlined.PhoneAndroid else Icons.Outlined.Screenshot,
            description = if (includeFrame) "Screenshot with frame" else "Screenshot of screen",
            tooltip = if (includeFrame) "Save the device in its frame as a PNG" else "Save the screen as a PNG at the device's resolution",
            onClick = {
                screenshots.capture(includeFrame)?.let { image ->
                    download(image, screenshotName(device, image, includeFrame))
                }
            },
            size = ButtonSize,
            tint = tint,
            hoverTint = hoverTint,
            enabled = screenshots.canCapture,
        )
    }
}
