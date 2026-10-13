package com.apadmi.cmpdevicepreview.frame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.apadmi.cmpdevicepreview.core.DeviceFrame
import com.apadmi.cmpdevicepreview.svg.Affine
import com.apadmi.cmpdevicepreview.svg.SvgDrawing
import com.apadmi.cmpdevicepreview.svg.parseSvgPathData

/**
 * Parses a [DeviceFrame] on first use and paints it. Malformed artwork is reported once and that
 * part of the frame is left undrawn.
 */
internal class DeviceFramePainter(val frame: DeviceFrame) {

    /** The parsed body artwork, or null when absent or malformed. */
    val body: SvgDrawing? by lazy(LazyThreadSafetyMode.NONE) {
        frame.body.takeIf { it.isNotEmpty() }?.let {
            reportingParseFailure("device frame body artwork") { SvgDrawing.parse(it) }
        }
    }

    /** The parsed screen outline in portrait screen coordinates, or null for a rectangular screen. */
    val screenPath: Path? by lazy(LazyThreadSafetyMode.NONE) {
        frame.screenPath.takeIf { it.isNotEmpty() }?.let {
            reportingParseFailure("device frame screen outline") { parseSvgPathData(it) }
        }
    }

    /** The screen outline for a screen of [screenSize], or null for a rectangular screen. */
    fun screenClip(screenSize: Size, isPortrait: Boolean): Path? {
        val path = screenPath ?: return null
        if (isPortrait) return path
        return Path().apply {
            addPath(path)
            transform(landscapeTransform(screenSize).toMatrix())
        }
    }

    /** Paints the body behind a screen of [screenSize], on a canvas whose origin is the screen's top-left corner. */
    fun paintBody(canvas: Canvas, screenSize: Size, isPortrait: Boolean) {
        val drawing = body ?: return
        canvas.save()
        if (!isPortrait) canvas.concat(landscapeTransform(screenSize).toMatrix())
        drawing.paintInto(canvas, Rect(Offset(-frame.screenOffset.x, -frame.screenOffset.y), frame.size))
        canvas.restore()
    }

    private companion object {
        /** A quarter turn onto a landscape screen of [screenSize]: portrait (x, y) → (y, portraitWidth − x). */
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
