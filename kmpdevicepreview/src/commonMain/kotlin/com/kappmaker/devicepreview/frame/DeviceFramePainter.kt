package com.kappmaker.devicepreview.frame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.kappmaker.devicepreview.core.DeviceFrame
import com.kappmaker.devicepreview.svg.Affine
import com.kappmaker.devicepreview.svg.SvgDrawing
import com.kappmaker.devicepreview.svg.parseSvgPathData

/**
 * Parses a [DeviceFrame] once and paints it.
 *
 * Path data and SVG artwork are parsed lazily on first use and cached for the painter's lifetime.
 * Malformed artwork never breaks the previewed content: the failure is reported once and that part
 * of the frame is simply not drawn.
 */
internal class DeviceFramePainter(val frame: DeviceFrame) {

    /** The parsed body artwork, or null when absent or malformed. */
    val body: SvgDrawing? by lazy(LazyThreadSafetyMode.NONE) {
        frame.body.takeIf { it.isNotEmpty() }?.let { guard("body artwork") { SvgDrawing.parse(it) } }
    }

    /**
     * The parsed screen outline in portrait screen coordinates, or null when absent or malformed
     * (a rectangular screen).
     */
    val screenPath: Path? by lazy(LazyThreadSafetyMode.NONE) {
        frame.screenPath.takeIf { it.isNotEmpty() }?.let { guard("screen outline") { parseSvgPathData(it) } }
    }

    /**
     * The screen clip outline for a screen of [screenSize] (already orientation-resolved), in the
     * content's own coordinates, or null for a rectangular screen.
     */
    fun screenClip(screenSize: Size, isPortrait: Boolean): Path? {
        val path = screenPath ?: return null
        if (isPortrait) return path
        return Path().apply {
            addPath(path)
            transform(landscapeTransform(screenSize).toMatrix())
        }
    }

    /**
     * Paints the body artwork behind a screen of [screenSize] (already orientation-resolved).
     *
     * The canvas origin must be the screen's top-left corner, in logical pixels.
     */
    fun paintBody(canvas: Canvas, screenSize: Size, isPortrait: Boolean) {
        val drawing = body ?: return
        if (drawing.isEmpty) return
        canvas.save()
        if (!isPortrait) {
            // A quarter turn: portrait (x, y) → landscape (y, portraitWidth − x).
            canvas.translate(0f, screenSize.height)
            canvas.rotate(-90f)
        }
        drawing.paintInto(canvas, Rect(Offset(-frame.screenOffset.x, -frame.screenOffset.y), frame.size))
        canvas.restore()
    }

    private fun <T : Any> guard(what: String, parse: () -> T): T? = try {
        parse()
    } catch (error: Exception) {
        println("DevicePreview: could not parse the device frame $what, it will not be drawn: $error")
        null
    }

    private companion object {
        /** The same quarter turn as [paintBody], for a landscape screen of [screenSize]. */
        fun landscapeTransform(screenSize: Size): Affine =
            Affine(a = 0f, b = -1f, c = 1f, d = 0f, e = 0f, f = screenSize.height)
    }
}

/** Clips content to a device's screen [outline], given in logical pixels for [screenSize]. */
internal class DeviceScreenShape(private val outline: Path, private val screenSize: Size) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            addPath(outline)
            transform(Affine.scale(size.width / screenSize.width, size.height / screenSize.height).toMatrix())
        }
        return Outline.Generic(path)
    }
}
