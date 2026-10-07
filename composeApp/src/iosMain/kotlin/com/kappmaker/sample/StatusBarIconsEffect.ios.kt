package com.kappmaker.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.setStatusBarStyle

@Composable
actual fun StatusBarIconsEffect(lightIcons: Boolean) {
    SideEffect {
        // Compose's own hook, ComposeUIViewControllerDelegate.preferredStatusBarStyle, only works
        // when UIKit asks the Compose view controller; SwiftUI hosts it here, so this sets the
        // app-wide style instead (UIViewControllerBasedStatusBarAppearance is NO in Info.plist).
        @Suppress("DEPRECATION")
        UIApplication.sharedApplication.setStatusBarStyle(
            if (lightIcons) UIStatusBarStyleLightContent else UIStatusBarStyleDarkContent,
            animated = true,
        )
    }
}
