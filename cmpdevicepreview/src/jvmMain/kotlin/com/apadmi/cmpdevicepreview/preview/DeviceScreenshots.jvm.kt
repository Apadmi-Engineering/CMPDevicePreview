package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toAwtImage
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

import java.awt.Dialog
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.File

internal actual val pngDownloader: PngDownloader? = { image, fileName ->
    // Once the click is handled: the dialog runs an event loop of its own.
    EventQueue.invokeLater {
        chooseFile(fileName)?.let { file -> image.toPngBytes()?.let(file::writeBytes) }
    }
}

internal actual val pngCopier: PngCopier? = { image, onDone ->
    // The clipboard can be busy with another app, or missing on a headless JVM.
    val isCopied = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(imageSelection(image.toAwtImage()), null)
    }.isSuccess
    onDone(isCopied)
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

private fun imageSelection(image: BufferedImage): Transferable = object : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor

    override fun getTransferData(flavor: DataFlavor): Any =
        if (isDataFlavorSupported(flavor)) image else throw UnsupportedFlavorException(flavor)
}
