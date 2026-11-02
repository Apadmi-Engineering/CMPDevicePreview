@file:OptIn(ExperimentalWasmJsInterop::class)

package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

internal actual val pngDownloader: PngDownloader? = { image, fileName ->
    image.toPngJsBytes()?.let { download(it, fileName) }
}

@Suppress("NULLABLE_PROPERTY_TYPE")  // Null where the browser can't put images on the clipboard, such as on a page not served over HTTPS.
internal actual val pngCopier: PngCopier? = if (canCopyImages()) {
    { image, onDone ->
        val png = image.toPngJsBytes()
        if (png == null) onDone(false) else copy(png, onDone)
    }
} else {
    null
}

private fun ImageBitmap.toPngJsBytes(): JsAny? = Image.makeFromBitmap(asSkiaBitmap())
    .encodeToData(EncodedImageFormat.PNG)
    ?.bytes
    ?.let { png ->
        val bytes = newBytes(png.size)
        png.forEachIndexed { index, byte -> setByte(bytes, index, byte.toInt()) }
        bytes
    }

private fun newBytes(size: Int): JsAny = js("new Uint8Array(size)")

// A Uint8Array keeps the low eight bits, so a negative byte is stored as the unsigned one.
private fun setByte(
    bytes: JsAny,
    index: Int,
    value: Int
): Unit = js("bytes[index] = value")

/** Has the browser download [bytes] as a PNG called [fileName], through a link clicked in code. */
private fun download(bytes: JsAny, fileName: String): Unit = js(
    """(() => {
        const url = URL.createObjectURL(new Blob([bytes], { type: 'image/png' }));
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    })()"""
)

private fun canCopyImages(): Boolean = js("typeof ClipboardItem === 'function' && !!navigator.clipboard && typeof navigator.clipboard.write === 'function'")

private fun copy(bytes: JsAny, onDone: (Boolean) -> Unit): Unit = js(
    """(() => {
        const item = new ClipboardItem({ 'image/png': new Blob([bytes], { type: 'image/png' }) });
        navigator.clipboard.write([item]).then(() => onDone(true), () => onDone(false));
    })()"""
)
