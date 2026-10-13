package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import com.apadmi.cmpdevicepreview.core.Device
import kotlin.math.roundToInt

/** E.g. "402 × 874". */
internal fun Size.dimensionsLabel(): String = "${width.roundToInt()} × ${height.roundToInt()}"

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
 * E.g. "Galaxy S25 1080x2340.png", or "Galaxy S25 with frame.png". Characters that aren't allowed
 * in file names, such as the inch mark in `iPad Pro 13" (M5)`, are left out.
 */
internal fun screenshotName(device: Device, image: ImageBitmap, includesFrame: Boolean): String {
    val name = device.name.filterNot { it in "\\/:*?\"<>|" }
    return if (includesFrame) "$name with frame.png" else "$name ${image.width}x${image.height}.png"
}
