package com.kappmaker.devicepreview.preview

import androidx.compose.ui.graphics.ImageBitmap

internal actual val pngDownloader: ((image: ImageBitmap, fileName: String) -> Unit)? = null
