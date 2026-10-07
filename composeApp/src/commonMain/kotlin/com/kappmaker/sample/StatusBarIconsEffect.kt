package com.kappmaker.sample

import androidx.compose.runtime.Composable

/**
 * Sets the colour of the status bar's icons the way an app normally does: through each platform's
 * own API, since Compose has no shared one. Use [lightIcons] over a dark background.
 *
 * The simulated status bar can't see these calls; its icons follow the simulated theme.
 */
@Composable
expect fun StatusBarIconsEffect(lightIcons: Boolean)
