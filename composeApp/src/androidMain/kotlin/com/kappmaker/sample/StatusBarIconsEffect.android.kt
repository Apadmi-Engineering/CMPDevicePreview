package com.kappmaker.sample

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun StatusBarIconsEffect(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as Activity).window
        // A "light status bar" is a light background, so it gets dark icons. There's no
        // window.statusBarColor here: Android 15 ignores it, so apps draw behind the bar instead.
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !lightIcons
    }
}
