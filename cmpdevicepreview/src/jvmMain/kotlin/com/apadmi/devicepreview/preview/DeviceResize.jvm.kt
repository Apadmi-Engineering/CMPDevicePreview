package com.apadmi.devicepreview.preview

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

internal actual fun resizeCursor(handle: ResizeHandle): PointerIcon = PointerIcon(
    Cursor(
        when (handle) {
            ResizeHandle(-1, -1) -> Cursor.NW_RESIZE_CURSOR
            ResizeHandle(1, -1) -> Cursor.NE_RESIZE_CURSOR
            ResizeHandle(1, 1) -> Cursor.SE_RESIZE_CURSOR
            ResizeHandle(-1, 1) -> Cursor.SW_RESIZE_CURSOR
            ResizeHandle(0, -1) -> Cursor.N_RESIZE_CURSOR
            ResizeHandle(0, 1) -> Cursor.S_RESIZE_CURSOR
            ResizeHandle(-1, 0) -> Cursor.W_RESIZE_CURSOR
            else -> Cursor.E_RESIZE_CURSOR
        }
    )
)
