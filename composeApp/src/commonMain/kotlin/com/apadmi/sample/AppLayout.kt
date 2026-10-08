package com.apadmi.sample

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toSize
import androidx.window.core.layout.WindowSizeClass

/** The app's layouts, one per Material window size class it handles, labelled with that class. */
enum class AppLayout(val label: String, val hasDetailPane: Boolean = false) {
    /** Compact width: a single column and a bottom navigation bar. */
    Phone("Compact"),

    /** Compact height, as on a phone turned sideways: a rail, and content side by side. */
    PhoneLandscape("Compact height"),

    /** Medium width: a rail and a grid. */
    Tablet("Medium"),

    /** Expanded width: a rail, a grid and a detail pane. */
    Desktop("Expanded", hasDetailPane = true),

    /** Large width: a navigation drawer, a grid and a wider detail pane. */
    LargeDesktop("Large", hasDetailPane = true),
}

@Composable
fun currentAppLayout(): AppLayout {
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    return when {
        !sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> AppLayout.PhoneLandscape
        !sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> AppLayout.Phone
        !sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> AppLayout.Tablet
        !sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> AppLayout.Desktop
        else -> AppLayout.LargeDesktop
    }
}

/** The window's size, for showing which breakpoint the app is at. */
@Composable
fun currentWindowSize(): DpSize = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.toSize().toDpSize()
}
