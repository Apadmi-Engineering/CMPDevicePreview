package com.apadmi.cmpdevicepreview.svg

import androidx.compose.ui.graphics.Matrix
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * A 2D affine transform, laid out like SVG's `matrix(a b c d e f)`:
 *
 * ```
 * x' = a·x + c·y + e
 * y' = b·x + d·y + f
 * ```
 *
 * @property a How much x adds to x'.
 * @property b How much x adds to y'.
 * @property c How much y adds to x'.
 * @property d How much y adds to y'.
 * @property e How far x' is translated.
 * @property f How far y' is translated.
 */
@Suppress("IDENTIFIER_LENGTH")  // The components are named as in SVG's matrix(a b c d e f).
internal data class Affine(
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val e: Float,
    val f: Float,
) {
    val isIdentity: Boolean
        get() = this == Identity
    /**
     * The composition that applies [other] first, then this transform. This is how an SVG
     * `transform` list composes left to right, and how a child's transform nests in its parent's.
     *
     * @param other The transform to apply first.
     * @return The combined transform.
     */
    operator fun times(other: Affine): Affine = Affine(
        a = a * other.a + c * other.b,
        b = b * other.a + d * other.b,
        c = a * other.c + c * other.d,
        d = b * other.c + d * other.d,
        e = a * other.e + c * other.f + e,
        f = b * other.e + d * other.f + f,
    )

    fun toMatrix(): Matrix = Matrix(
        floatArrayOf(
            a, b, 0f, 0f,
            c, d, 0f, 0f,
            0f, 0f, 1f, 0f,
            e, f, 0f, 1f,
        )
    )

    companion object {
        val Identity: Affine = Affine(1f, 0f, 0f, 1f, 0f, 0f)

        fun translate(x: Float, y: Float): Affine = Affine(1f, 0f, 0f, 1f, x, y)

        fun scale(x: Float, y: Float = x): Affine = Affine(x, 0f, 0f, y, 0f, 0f)

        fun rotate(degrees: Float): Affine {
            val radians = degrees * PI / 180
            val cos = cos(radians).toFloat()
            val sin = sin(radians).toFloat()
            return Affine(cos, sin, -sin, cos, 0f, 0f)
        }

        fun skewX(degrees: Float): Affine = Affine(1f, 0f, tan(degrees * PI / 180).toFloat(), 1f, 0f, 0f)

        fun skewY(degrees: Float): Affine = Affine(1f, tan(degrees * PI / 180).toFloat(), 0f, 1f, 0f, 0f)
    }
}
