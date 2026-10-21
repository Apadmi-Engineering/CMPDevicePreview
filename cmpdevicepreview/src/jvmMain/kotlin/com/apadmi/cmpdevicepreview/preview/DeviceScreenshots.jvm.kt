package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

import java.awt.Dialog
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.io.File

internal actual val pngDownloader: PngDownloader? = { image, fileName ->
    // Once the click is handled: the dialog runs an event loop of its own.
    EventQueue.invokeLater {
        chooseFile(fileName)?.let { file -> image.toPngBytes()?.let(file::writeBytes) }
    }
}

internal fun ImageBitmap.toPngBytes(): ByteArray? = Image.makeFromBitmap(asSkiaBitmap())
    .encodeToData(EncodedImageFormat.PNG)
    ?.bytes

private fun chooseFile(suggestedName: String): File? {
    val dialog = saveDialog().apply {
        file = suggestedName
        // Blocks until the user has chosen a file or cancelled.
        isVisible = true
    }
    return dialog.file?.let { File(dialog.directory, it) }
}

private fun saveDialog(): FileDialog {
    val title = "Save screenshot"
    return when (val owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow) {
        is Dialog -> FileDialog(owner, title, FileDialog.SAVE)
        is Frame -> FileDialog(owner, title, FileDialog.SAVE)
        else -> FileDialog(null as Frame?, title, FileDialog.SAVE)
    }
}
