package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SwipeVertical
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.apadmi.cmpdevicepreview.core.Device

import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private const val ZoomStep = 1.25f
private const val RestingAlpha = 0.6f
private const val HoveredAlpha = 0.9f
private const val NoticeMillis = 2000L

private val ButtonSize = 28.dp
private val MenuWidth = 180.dp

@Composable
internal fun PreviewToolbar(
    viewport: PreviewViewport,
    isBackdropDark: Boolean,
    modifier: Modifier = Modifier,
    isDragToScroll: Boolean? = null,
    onDragToScrollChange: (Boolean) -> Unit = {},
) {
    val (resting, hovered) = toolbarTints(isBackdropDark)
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
                size = ButtonSize, tint = resting, hoverTint = hovered, enabled = viewport.isTransformed,
            )
            isDragToScroll?.let { DragToScrollSwitch(it, onDragToScrollChange, resting, hovered) }
        }
    }
}

@Composable
internal fun ScreenshotMenu(
    device: Device,
    isBackdropDark: Boolean,
    modifier: Modifier = Modifier,
    download: PngDownloader? = pngDownloader,
    copy: PngCopier? = pngCopier,
) {
    val screenshots = LocalDeviceScreenshots.current
    if (screenshots == null || download == null) {
        return
    }
    val (resting, hovered) = toolbarTints(isBackdropDark)
    var isOpen by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var noticeCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(noticeCount) {
        delay(NoticeMillis)
        notice = null
    }
    val take = { includeFrame: Boolean, use: (ImageBitmap) -> Unit ->
        isOpen = false
        screenshots.capture(includeFrame)?.let(use)
    }
    PanelTheme {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            notice?.let {
                Text(
                    it,
                    modifier = Modifier.padding(end = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    color = resting,
                    fontSize = 11.sp,
                )
            }
            Box {
                PanelIconButton(
                    icon = Icons.Outlined.PhotoCamera,
                    description = "Screenshot",
                    tooltip = "Screenshot",
                    onClick = { isOpen = true },
                    size = ButtonSize,
                    tint = resting,
                    hoverTint = hovered,
                    enabled = screenshots.canCapture,
                )
                // Lined up with the button's end, so it opens over the canvas: over the panel, clicks went through to the panel.
                DropdownMenu(
                    expanded = isOpen,
                    onDismissRequest = { isOpen = false },
                    modifier = Modifier.width(MenuWidth),
                    offset = DpOffset(ButtonSize - MenuWidth, 0.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = PanelColors.SurfaceRaised,
                    border = BorderStroke(1.dp, PanelColors.Border),
                ) {
                    for (includeFrame in listOf(false, true)) {
                        ScreenshotMenuItem(if (includeFrame) "Save with frame" else "Save screen", Icons.Outlined.Download) {
                            take(includeFrame) { image -> download(image, screenshotName(device, image, includeFrame)) }
                        }
                    }
                    if (copy != null) {
                        HorizontalDivider(color = PanelColors.Border)
                        for (includeFrame in listOf(false, true)) {
                            ScreenshotMenuItem(if (includeFrame) "Copy with frame" else "Copy screen", Icons.Outlined.ContentCopy) {
                                take(includeFrame) { image ->
                                    copy(image) { isCopied ->
                                        notice = if (isCopied) "Copied" else "Couldn't copy"
                                        noticeCount++
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DragToScrollSwitch(
    isDragToScroll: Boolean,
    onChange: (Boolean) -> Unit,
    tint: Color,
    hoverTint: Color
) {
    Spacer(Modifier.width(8.dp))
    PanelIconButton(
        icon = Icons.Outlined.SwipeVertical,
        description = "Drag to scroll",
        tooltip = if (isDragToScroll) "Drag to scroll: on" else "Drag to scroll: off",
        onClick = { onChange(!isDragToScroll) },
        size = ButtonSize,
        tint = tint,
        hoverTint = hoverTint,
        isChecked = isDragToScroll,
    )
}

@Composable
private fun ScreenshotMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text, color = PanelColors.TextPrimary, fontSize = 14.sp) },
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null, tint = PanelColors.TextSecondary, modifier = Modifier.size(18.dp)) },
    )
}

// The toolbars' ink at rest and under the pointer: black on a light backdrop, white on a dark one.
private fun toolbarTints(isBackdropDark: Boolean): Pair<Color, Color> {
    val ink = if (isBackdropDark) Color.White else Color.Black
    return ink.copy(alpha = RestingAlpha) to ink.copy(alpha = HoveredAlpha)
}
