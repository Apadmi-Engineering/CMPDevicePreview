package com.apadmi.cmpdevicepreview.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Suppress("NULLABLE_PROPERTY_TYPE")  // Null where there's no browser to draw the card, so Explore leaves it out.
actual val embeddedHtmlCard: (@Composable (Modifier) -> Unit)? = null
