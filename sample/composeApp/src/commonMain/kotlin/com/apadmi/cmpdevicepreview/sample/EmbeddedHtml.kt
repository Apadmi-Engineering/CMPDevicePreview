package com.apadmi.cmpdevicepreview.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// A card the browser draws, shown on Explore: null where there's no browser to draw it.
expect val embeddedHtmlCard: (@Composable (Modifier) -> Unit)?
