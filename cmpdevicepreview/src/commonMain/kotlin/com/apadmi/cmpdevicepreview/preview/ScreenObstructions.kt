package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.toSize

internal val LocalScreenObstructions = staticCompositionLocalOf<ScreenObstructions?> { null }

// A dialog or popup is drawn in a layer over the whole window, so it covers whatever the preview
// draws over the device, such as its toolbars, unless the layer leaves them out. The outlines are in
// the window's pixels.
@Stable
internal class ScreenObstructions {
    private val outlines = mutableStateMapOf<Any, Path>()

    val paths: Collection<Path> get() = outlines.values

    fun set(key: Any, outline: Path) {
        outlines[key] = outline
    }

    fun remove(key: Any) {
        outlines.remove(key)
    }
}

@Composable
internal fun Modifier.obstructingScreenLayers(shape: Shape): Modifier {
    val obstructions = LocalScreenObstructions.current ?: return this
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val key = remember { Any() }
    DisposableEffect(obstructions, key) {
        onDispose { obstructions.remove(key) }
    }
    return onGloballyPositioned { coordinates ->
        val outline = Path().apply {
            addOutline(shape.createOutline(coordinates.size.toSize(), layoutDirection, density))
            translate(coordinates.positionInWindow())
        }
        obstructions.set(key, outline)
    }
}
