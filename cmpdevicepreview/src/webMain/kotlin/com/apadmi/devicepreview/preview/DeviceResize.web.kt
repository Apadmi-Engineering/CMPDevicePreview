package com.apadmi.devicepreview.preview

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.fromKeyword

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun resizeCursor(handle: ResizeHandle): PointerIcon = PointerIcon.fromKeyword(
    when {
        handle.x == 0 -> "ns-resize"
        handle.y == 0 -> "ew-resize"
        handle.x == handle.y -> "nwse-resize"
        else -> "nesw-resize"
    }
)
