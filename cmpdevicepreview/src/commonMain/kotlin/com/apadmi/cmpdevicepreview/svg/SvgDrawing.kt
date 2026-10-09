package com.apadmi.cmpdevicepreview.svg

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.roundToInt

/**
 * A single flattened, ready-to-paint shape of an [SvgDrawing].
 *
 * The path is already expressed in the drawing's own (view box) coordinate space: every ancestor
 * `transform` has been baked in at parse time, so painting is a flat loop with no state to unwind.
 *
 * @property color The resolved fill color, including `fill-opacity` and inherited `opacity`.
 * @property clips Clip outlines to intersect before filling, outermost first.
 * @property followsCurrentColor Whether this shape inherited its fill (`fill="currentColor"`, or no
 * fill at all), and so follows the `currentColor` passed to [SvgDrawing.paint].
 */
internal class SvgShape(
    val path: Path,
    val color: Color,
    val clips: List<Path> = emptyList(),
    val followsCurrentColor: Boolean = false,
)

/**
 * A parsed rendering of the small SVG subset device artwork needs.
 *
 * ## Supported subset
 *
 * * Structure: `<svg>` (`viewBox`, `width`/`height`), `<g>`, `<defs>`, `<clipPath>`.
 * * Shapes: `<path>`, `<rect>` (including `rx`/`ry`), `<circle>`, `<ellipse>`, `<polygon>`.
 * * Paint: `fill` (`#rgb`, `#rgba`, `#rrggbb`, `#rrggbbaa`, `rgb()`, `rgba()`, a handful of named
 *   colors, `none`), `fill-opacity`, `fill-rule`, `opacity`, and the same properties written
 *   inside a `style="…"` attribute.
 * * Geometry: `transform` (`matrix`, `translate`, `scale`, `rotate`, `skewX`, `skewY`) and
 *   `clip-path="url(#id)"`.
 *
 * Everything else — strokes, gradients, filters, text, images, masks — is ignored by design.
 * Group `opacity` is multiplied into descendant fills rather than composited through a layer,
 * which is exact for non-overlapping flat artwork.
 *
 * Ported from flutter_device_preview's `SvgDrawing`, which device specs are authored against.
 */
internal class SvgDrawing(val viewBox: Rect, val shapes: List<SvgShape>) {

    /** The size of [viewBox]. */
    val size: Size
        get() = viewBox.size

    /** Whether this drawing paints nothing. */
    val isEmpty: Boolean
        get() = shapes.isEmpty()

    /**
     * Paints the drawing in its own view box coordinates.
     *
     * [currentColor] tints every shape that inherited its fill, preserving the shape's own opacity.
     */
    fun paint(canvas: Canvas, currentColor: Color? = null) {
        val paint = Paint().apply { isAntiAlias = true }
        for (shape in shapes) {
            paint.color = if (currentColor != null && shape.followsCurrentColor) {
                currentColor.copy(alpha = currentColor.alpha * shape.color.alpha)
            } else {
                shape.color
            }
            if (shape.clips.isEmpty()) {
                canvas.drawPath(shape.path, paint)
                continue
            }
            canvas.save()
            for (clip in shape.clips) canvas.clipPath(clip)
            canvas.drawPath(shape.path, paint)
            canvas.restore()
        }
    }

    /**
     * Paints the drawing stretched onto [destination].
     *
     * The view box is mapped onto the destination without preserving the aspect ratio — device
     * bodies declare a view box that already matches their target, and an exact mapping keeps the
     * screen cut-out aligned with the simulated screen.
     */
    fun paintInto(canvas: Canvas, destination: Rect, currentColor: Color? = null) {
        if (shapes.isEmpty() || viewBox.isEmpty || destination.isEmpty) return
        canvas.save()
        canvas.translate(destination.left, destination.top)
        canvas.scale(destination.width / viewBox.width, destination.height / viewBox.height)
        canvas.translate(-viewBox.left, -viewBox.top)
        paint(canvas, currentColor)
        canvas.restore()
    }

    companion object {
        /**
         * Parses an SVG document.
         *
         * @throws IllegalArgumentException when the document is not well-formed XML, is not
         * rooted at `<svg>`, or carries neither a `viewBox` nor `width`/`height`.
         */
        fun parse(source: String): SvgDrawing {
            val root = parseXmlDocument(source)
            require(root.name == "svg") { "Expected a root <svg> element, got <${root.name}>" }
            val viewBox = parseViewBox(root)
            val clipPaths = mutableMapOf<String, List<XmlElement>>()
            collectClipPaths(root, clipPaths)
            val shapes = mutableListOf<SvgShape>()
            Flattener(clipPaths, shapes).visitChildren(
                root,
                InheritedStyle(
                    transform = Affine.Identity,
                    // The SVG initial value of `fill`.
                    color = Color.Black,
                    opacity = 1f,
                    clips = emptyList(),
                ),
            )
            return SvgDrawing(viewBox, shapes)
        }

        private fun parseViewBox(root: XmlElement): Rect {
            root.attribute("viewBox")?.let { raw ->
                val values = parseNumberList(raw)
                require(values.size == 4) { "Expected four numbers in viewBox, got: $raw" }
                return Rect(Offset(values[0], values[1]), Size(values[2], values[3]))
            }
            val width = parseLength(root.attribute("width"))
            val height = parseLength(root.attribute("height"))
            require(width != null && height != null) {
                "The root <svg> element needs a viewBox or width/height attributes"
            }
            return Rect(0f, 0f, width, height)
        }

        private fun collectClipPaths(element: XmlElement, into: MutableMap<String, List<XmlElement>>) {
            for (child in element.children) {
                if (child.name == "clipPath") {
                    child.attribute("id")?.let { id -> into[id] = child.children }
                }
                collectClipPaths(child, into)
            }
        }
    }
}

/**
 * Parses SVG path data (the `d` attribute of `<path>`) into a [Path], using Compose's own parser:
 * the full command set, arcs included.
 */
internal fun parseSvgPathData(data: String, fillType: PathFillType = PathFillType.NonZero): Path =
    PathParser().parsePathString(data).toPath().apply { this.fillType = fillType }

/** Style inherited down the element tree while flattening. */
private class InheritedStyle(
    val transform: Affine,
    /** The inherited `fill`, or null for `fill="none"`. */
    val color: Color?,
    val opacity: Float,
    val clips: List<Path>,
    /** Whether [color] came from inheritance rather than an explicit fill. */
    val inheritsColor: Boolean = true,
)

/** Walks the element tree once, resolving styles and baking transforms into absolute paths. */
private class Flattener(
    private val clipPaths: Map<String, List<XmlElement>>,
    private val shapes: MutableList<SvgShape>,
) {

    fun visitChildren(element: XmlElement, style: InheritedStyle) {
        for (child in element.children) visit(child, style)
    }

    private fun visit(element: XmlElement, parent: InheritedStyle) {
        if (element.name in skipped) return
        val attributes = resolveAttributes(element)
        val transform = valueOf(attributes, "transform")
            ?.let { parent.transform * parseTransform(it) }
            ?: parent.transform
        val opacity = parent.opacity * (parseNumber(valueOf(attributes, "opacity")) ?: 1f)
        val rawFill = valueOf(attributes, "fill")
        // `none` is inheritable, so it is carried as a null color rather than handled at the leaf.
        val color: Color?
        var inheritsColor = parent.inheritsColor
        when (rawFill) {
            "currentColor" -> {
                // The paint-time tint, not the inherited fill, as CSS `color` is independent of
                // `fill`. Without a tint it is black, the initial value of `color`.
                color = Color.Black
                inheritsColor = true
            }
            null -> color = parent.color
            "none", "transparent" -> color = null
            else -> {
                val parsed = parseColor(rawFill)
                color = parsed ?: parent.color
                inheritsColor = parsed == null && parent.inheritsColor
            }
        }
        val clips = resolveClips(attributes, transform, parent)
        val style = InheritedStyle(transform, color, opacity, clips, inheritsColor)

        if (element.name in containers) {
            visitChildren(element, style)
            return
        }

        if (color == null) return
        val path = buildShape(element, attributes) ?: return
        if (!transform.isIdentity) path.transform(transform.toMatrix())
        val fillOpacity = parseNumber(valueOf(attributes, "fill-opacity")) ?: 1f
        val alpha = (opacity * fillOpacity).coerceIn(0f, 1f)
        shapes += SvgShape(
            path = path,
            color = color.copy(alpha = color.alpha * alpha),
            clips = clips,
            followsCurrentColor = inheritsColor,
        )
    }

    private fun resolveClips(attributes: Map<String, String>, transform: Affine, parent: InheritedStyle): List<Path> {
        val reference = valueOf(attributes, "clip-path") ?: return parent.clips
        val definition = parseUrlReference(reference)?.let { clipPaths[it] }
        if (definition.isNullOrEmpty()) return parent.clips
        // `clipPathUnits` defaults to `userSpaceOnUse`: the clip geometry lives in the user space
        // of the referencing element, i.e. under `transform`.
        val clip = Path()
        for (child in definition) {
            val childAttributes = resolveAttributes(child)
            val shape = buildShape(child, childAttributes) ?: continue
            valueOf(childAttributes, "transform")?.let { shape.transform(parseTransform(it).toMatrix()) }
            clip.addPath(shape)
        }
        if (!transform.isIdentity) clip.transform(transform.toMatrix())
        return parent.clips + clip
    }

    private fun buildShape(element: XmlElement, attributes: Map<String, String>): Path? {
        val fillType = if (valueOf(attributes, "fill-rule") == "evenodd") PathFillType.EvenOdd else PathFillType.NonZero
        fun number(name: String): Float? = parseNumber(valueOf(attributes, name))
        return when (element.name) {
            "path" -> valueOf(attributes, "d")?.let { parseSvgPathData(it, fillType) }
            "rect" -> {
                val width = number("width") ?: 0f
                val height = number("height") ?: 0f
                if (width <= 0f || height <= 0f) return null
                val rect = Rect(Offset(number("x") ?: 0f, number("y") ?: 0f), Size(width, height))
                val rx = number("rx")
                val ry = number("ry")
                Path().apply {
                    this.fillType = fillType
                    if (rx == null && ry == null) {
                        addRect(rect)
                    } else {
                        addRoundRect(RoundRect(rect, CornerRadius(rx ?: ry!!, ry ?: rx!!)))
                    }
                }
            }
            "circle" -> {
                val radius = number("r") ?: 0f
                if (radius <= 0f) return null
                Path().apply {
                    this.fillType = fillType
                    addOval(Rect(Offset(number("cx") ?: 0f, number("cy") ?: 0f), radius))
                }
            }
            "ellipse" -> {
                val rx = number("rx") ?: 0f
                val ry = number("ry") ?: 0f
                if (rx <= 0f || ry <= 0f) return null
                val cx = number("cx") ?: 0f
                val cy = number("cy") ?: 0f
                Path().apply {
                    this.fillType = fillType
                    addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry))
                }
            }
            "polygon" -> {
                val points = parseNumberList(valueOf(attributes, "points") ?: "")
                if (points.size < 6) return null
                Path().apply {
                    this.fillType = fillType
                    moveTo(points[0], points[1])
                    var i = 2
                    while (i + 1 < points.size) {
                        lineTo(points[i], points[i + 1])
                        i += 2
                    }
                    close()
                }
            }
            else -> null
        }
    }

    private companion object {
        /** Elements that only group children. */
        val containers = setOf("g", "a", "svg", "switch")

        /** Elements skipped entirely (definitions and metadata). */
        val skipped = setOf(
            "defs", "clipPath", "mask", "title", "desc", "metadata", "style", "symbol",
            "linearGradient", "radialGradient", "filter",
        )

        /** Merges presentation attributes with the `style="…"` declarations, which win. */
        fun resolveAttributes(element: XmlElement): Map<String, String> {
            val style = element.attribute("style") ?: return element.attributes
            val merged = element.attributes.toMutableMap()
            for (declaration in style.split(';')) {
                val separator = declaration.indexOf(':')
                if (separator <= 0) continue
                merged[declaration.substring(0, separator).trim()] = declaration.substring(separator + 1).trim()
            }
            return merged
        }

        fun valueOf(attributes: Map<String, String>, name: String): String? =
            attributes[name]?.takeIf { it.isNotEmpty() }?.trim()
    }
}

private val numberPattern = Regex("[-+]?(\\d*\\.\\d+|\\d+\\.?)([eE][-+]?\\d+)?")

/** Parses a whitespace/comma separated list of numbers. */
private fun parseNumberList(raw: String): List<Float> =
    numberPattern.findAll(raw).mapNotNull { it.value.toFloatOrNull() }.toList()

private fun parseNumber(raw: String?): Float? = raw?.trim()?.toFloatOrNull()

/** Parses a length, dropping a `px` suffix (other units are unsupported). */
private fun parseLength(raw: String?): Float? = raw?.removeSuffix("px")?.trim()?.toFloatOrNull()

private val urlPattern = Regex("url\\(\\s*#([^)\\s]+)\\s*\\)")

private fun parseUrlReference(raw: String): String? = urlPattern.find(raw)?.groupValues?.get(1)

private val rgbPattern = Regex("rgba?\\(([^)]*)\\)")

/** Parses the color syntaxes of the supported subset; null for anything else. */
private fun parseColor(raw: String): Color? {
    val value = raw.trim()
    if (value.isEmpty() || value == "none" || value == "transparent") return null
    if (value.startsWith("#")) {
        val hex = value.substring(1)
        return when (hex.length) {
            3, 4 -> parseHex(hex.map { "$it$it" }.joinToString(""))
            6, 8 -> parseHex(hex)
            else -> null
        }
    }
    rgbPattern.find(value)?.let { match ->
        val components = parseNumberList(match.groupValues[1])
        if (components.size < 3) return null
        val alpha = if (components.size > 3) components[3] else 1f
        return Color(
            red = components[0].roundToInt().coerceIn(0, 255),
            green = components[1].roundToInt().coerceIn(0, 255),
            blue = components[2].roundToInt().coerceIn(0, 255),
            alpha = (alpha.coerceIn(0f, 1f) * 255).roundToInt(),
        )
    }
    return namedColors[value.lowercase()]
}

private fun parseHex(hex: String): Color? {
    val value = hex.toLongOrNull(16) ?: return null
    // #rrggbb → 0xFFrrggbb; #rrggbbaa → 0xaarrggbb.
    return if (hex.length == 6) Color(0xFF000000 or value) else Color(((value and 0xFF) shl 24) or (value ushr 8))
}

private val namedColors = mapOf(
    "black" to Color(0xFF000000),
    "white" to Color(0xFFFFFFFF),
    "gray" to Color(0xFF808080),
    "grey" to Color(0xFF808080),
    "silver" to Color(0xFFC0C0C0),
    "red" to Color(0xFFFF0000),
    "green" to Color(0xFF008000),
    "blue" to Color(0xFF0000FF),
    "yellow" to Color(0xFFFFFF00),
    "orange" to Color(0xFFFFA500),
    "navy" to Color(0xFF000080),
)

private val transformPattern = Regex("(\\w+)\\s*\\(([^)]*)\\)")

/** Parses an SVG `transform` attribute; its operations apply right to left, as in SVG. */
private fun parseTransform(raw: String): Affine {
    var result = Affine.Identity
    for (match in transformPattern.findAll(raw)) {
        val values = parseNumberList(match.groupValues[2])
        val operation = when (match.groupValues[1]) {
            "matrix" -> if (values.size == 6) {
                Affine(values[0], values[1], values[2], values[3], values[4], values[5])
            } else {
                null
            }
            "translate" -> values.firstOrNull()?.let { Affine.translate(it, values.getOrElse(1) { 0f }) }
            "scale" -> values.firstOrNull()?.let { x -> Affine.scale(x, values.getOrElse(1) { x }) }
            "rotate" -> values.firstOrNull()?.let { degrees ->
                if (values.size >= 3) {
                    Affine.translate(values[1], values[2]) * Affine.rotate(degrees) * Affine.translate(-values[1], -values[2])
                } else {
                    Affine.rotate(degrees)
                }
            }
            "skewX" -> values.firstOrNull()?.let(Affine::skewX)
            "skewY" -> values.firstOrNull()?.let(Affine::skewY)
            else -> null
        }
        if (operation != null) result *= operation
    }
    return result
}
