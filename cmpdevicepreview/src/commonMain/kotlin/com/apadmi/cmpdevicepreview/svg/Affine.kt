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
 */
internal data class Affine(
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val e: Float,
    val f: Float,
) {

    /**
     * The composition that applies [other] first, then this transform — how an SVG `transform`
     * list composes left to right, and how a child's transform nests under its parent's.
     */
    operator fun times(other: Affine): Affine = Affine(
        a = a * other.a + c * other.b,
        b = b * other.a + d * other.b,
        c = a * other.c + c * other.d,
        d = b * other.c + d * other.d,
        e = a * other.e + c * other.f + e,
        f = b * other.e + d * other.f + f,
    )

    val isIdentity: Boolean
        get() = this == Identity

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
