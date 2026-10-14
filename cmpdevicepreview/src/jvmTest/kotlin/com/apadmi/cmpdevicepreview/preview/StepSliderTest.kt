package com.apadmi.cmpdevicepreview.preview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.renderComposeScene

import org.jetbrains.skia.Bitmap

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

class StepSliderTest {
    private val labels = (0..50).map(Int::toString)

    // The x of each run of pixels in the tick colour, below the setting's label.
    private fun tickPositions(marks: Set<Int>, selectedIndex: Int): List<Int> {
        val image = renderComposeScene(316, 120) {
            PanelTheme {
                StepSlider(Icons.Outlined.FormatSize, "Size", labels, selectedIndex, onSelect = {}, markedIndices = marks)
            }
        }
        val bitmap = Bitmap.makeFromImage(image)
        val tick = PanelColors.TextSecondary.toArgb()
        val xs = (0 until bitmap.width).filter { x -> (50 until bitmap.height).any { y -> bitmap.getColor(x, y) == tick } }
        return xs.fold(mutableListOf<MutableList<Int>>()) { runs, x ->
            if (runs.isEmpty() || x - runs.last().last() > 1) runs += mutableListOf(x) else runs.last() += x
            runs
        }.map { it.average().toInt() }
    }

    @Test
    fun onlyTheMarkedValuesGetATickWhenThereAreManyValues() {
        val positions = tickPositions(marks = setOf(7, 10, 13, 16), selectedIndex = 0)
        assertEquals(4, positions.size, "ticks at $positions")
        val spacing = positions.zipWithNext { left, right -> right - left }
        // Three values apart on a track of fifty steps.
        assertEquals(true, spacing.all { abs(it - 17) <= 1 }, "spacing $spacing")
    }

    @Test
    fun noValuesAreTickedWhenManyAndNoneMarked() {
        assertEquals(emptyList(), tickPositions(marks = emptySet(), selectedIndex = 0))
    }
}
