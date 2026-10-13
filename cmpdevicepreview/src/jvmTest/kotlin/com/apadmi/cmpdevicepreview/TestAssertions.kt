package com.apadmi.cmpdevicepreview

import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.test.fail

/**
 * Asserts that each side of [actual] is within [tolerance] of [expected]'s.
 *
 * @param expected The rectangle [actual] is compared against.
 * @param actual The rectangle under test.
 * @param tolerance The largest difference allowed on any one side.
 * @param message A prefix for the failure message, or null for none.
 */
internal fun assertRectEquals(
    expected: Rect,
    actual: Rect,
    tolerance: Float,
    message: String? = null
) {
    val sides = listOf(
        expected.left to actual.left,
        expected.top to actual.top,
        expected.right to actual.right,
        expected.bottom to actual.bottom,
    )
    if (sides.any { (expectedSide, actualSide) -> abs(expectedSide - actualSide) > tolerance }) {
        fail("${message?.let { "$it: " }.orEmpty()}expected $expected, got $actual")
    }
}
