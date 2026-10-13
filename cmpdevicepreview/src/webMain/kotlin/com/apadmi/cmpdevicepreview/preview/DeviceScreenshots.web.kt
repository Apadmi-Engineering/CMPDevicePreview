@file:OptIn(ExperimentalWasmJsInterop::class)

package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

internal actual val pngDownloader: PngDownloader? = { image, fileName ->
    Image.makeFromBitmap(image.asSkiaBitmap())
        .encodeToData(EncodedImageFormat.PNG)
        ?.bytes
        ?.let { png ->
            val bytes = newBytes(png.size)
            png.forEachIndexed { index, byte -> setByte(bytes, index, byte.toInt()) }
            download(bytes, fileName)
        }
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
