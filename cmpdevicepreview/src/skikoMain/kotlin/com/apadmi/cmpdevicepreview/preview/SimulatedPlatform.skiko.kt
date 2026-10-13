@file:OptIn(InternalComposeUiApi::class)

package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.platform.PlatformWindowInsetsProviderNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.apadmi.cmpdevicepreview.core.DeviceInsets

/**
 * The simulated device's insets: the top of its safe area is the status bar, the bottom the
 * navigation bar or home indicator, and the sides (a landscape notch) the display cutout.
 *
 * @property safeArea The device's safe area, in the content's dp.
 * @property keyboardHeight The height of the keyboard, in the content's dp, reported as the IME
 * inset.
 * @property density The content's density, which turns the dp above into the pixels reported.
 */
private data class SimulatedWindowInsets(
    val safeArea: DeviceInsets,
    val keyboardHeight: Float,
    val density: Density,
) : PlatformWindowInsets {
    override val statusBars: PlatformInsets = insets(top = safeArea.top)
    override val navigationBars: PlatformInsets = insets(bottom = safeArea.bottom)
    override val displayCutout: PlatformInsets = insets(left = safeArea.left, right = safeArea.right)
    override val systemBars: PlatformInsets = insets(top = safeArea.top, bottom = safeArea.bottom)
    override val tappableElement: PlatformInsets = systemBars
    override val systemGestures: PlatformInsets = navigationBars
    override val mandatorySystemGestures: PlatformInsets = navigationBars
    override val ime: PlatformInsets = insets(bottom = keyboardHeight)

    /** Insets given in the device's logical pixels. */
    private fun insets(
        left: Float = 0f,
        top: Float = 0f,
        right: Float = 0f,
        bottom: Float = 0f
    ): PlatformInsets =
        with(density) {
            PixelInsets(left.dp.roundToPx(), top.dp.roundToPx(), right.dp.roundToPx(), bottom.dp.roundToPx())
        }
}

private data class PixelInsets(
    override val left: Int,
    override val top: Int,
    override val right: Int,
    override val bottom: Int,
) : PlatformInsets

/**
 * Gives the modifiers below it [insets] to read, such as `safeDrawingPadding()` and `imePadding()`.
 *
 * @property insets The insets given to the modifiers below, in place of those of the ancestors.
 */
private data class SimulatedWindowInsetsElement(
    val insets: PlatformWindowInsets,
) : ModifierNodeElement<SimulatedWindowInsetsNode>() {
    override fun create(): SimulatedWindowInsetsNode = SimulatedWindowInsetsNode(insets)

    override fun update(node: SimulatedWindowInsetsNode) = node.update(insets)
}

private class SimulatedWindowInsetsNode(private var insets: PlatformWindowInsets) : PlatformWindowInsetsProviderNode() {
    override fun calculatePlatformInsets(ancestorWindowInsets: PlatformWindowInsets): PlatformWindowInsets = insets

    fun update(insets: PlatformWindowInsets) {
        if (insets != this.insets) {
            this.insets = insets
            windowInsetsInvalidated()
        }
    }
}

// Compose Multiplatform's hooks for these are internal, and the theme one is deprecated: this file
// is the only place that uses them, so a Compose update that changes them only breaks this file.

@Composable
internal actual fun SimulatedPlatform(
    safeArea: DeviceInsets,
    keyboardHeight: Float,
    isDarkMode: Boolean,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val insets = remember(safeArea, keyboardHeight, density) {
        SimulatedWindowInsets(safeArea, keyboardHeight, density)
    }
    @Suppress("DEPRECATION")
    CompositionLocalProvider(
        // Read in composition: `WindowInsets.safeDrawing`, `WindowInsets.ime`, Scaffold's defaults.
        LocalPlatformWindowInsets provides insets,
        LocalSystemTheme provides if (isDarkMode) SystemTheme.Dark else SystemTheme.Light,
    ) {
        // Read by modifiers such as `safeDrawingPadding()` and `imePadding()`, from the nearest
        // provider above them.
        Box(SimulatedWindowInsetsElement(insets)) {
            content()
        }
    }
}
