package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType

// A scene on a clock of its own, so dialogs and sheets finish appearing between a test's steps.
internal abstract class SceneHarness {
    private var clock = 0L
    abstract val scene: ImageComposeScene

    // A second of frames, long enough for a dialog or sheet to finish appearing, then the last of them.
    fun settle(): PixelMap {
        repeat(60) {
            clock += 16
            scene.render(clock * 1_000_000)
        }
        return scene.render(clock * 1_000_000).toComposeImageBitmap().toPixelMap()
    }

    fun tap(at: Offset) {
        press(at)
        release(at)
        settle()
    }

    fun press(at: Offset) = send(PointerEventType.Press, at, PointerType.Touch)

    fun release(at: Offset) = send(PointerEventType.Release, at, PointerType.Touch)

    fun hover(at: Offset) = send(PointerEventType.Move, at, PointerType.Mouse)

    fun scroll(
        at: Offset,
        delta: Offset,
        isCtrlPressed: Boolean = false
    ) {
        clock += 16
        scene.sendPointerEvent(
            PointerEventType.Scroll,
            at,
            scrollDelta = delta,
            timeMillis = clock,
            keyboardModifiers = PointerKeyboardModifiers(isCtrlPressed = isCtrlPressed),
        )
    }

    private fun send(
        eventType: PointerEventType,
        at: Offset,
        pointerType: PointerType
    ) {
        clock += 16
        scene.sendPointerEvent(eventType, at, timeMillis = clock, type = pointerType)
    }
}
