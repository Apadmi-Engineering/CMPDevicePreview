package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun DevicePreviewLogo(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    shape: Shape = RoundedCornerShape(size * 2 / 7),
    contentDescription: String? = null,
) {
    Box(
        modifier
            .size(size)
            .background(Brush.linearGradient(listOf(PanelColors.Accent, PanelColors.AccentBlue)), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.CropFree, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(size * 4 / 7))
    }
}
