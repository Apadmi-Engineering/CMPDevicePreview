package com.kappmaker.devicepreview.core

import kotlin.math.abs
import kotlin.math.min

/** A value a device's settings offer, such as a text size, with the name the panel gives it. */
internal class ScaleStep(val label: String, val scale: Float)

/**
 * The text sizes the panel offers, smallest first, as font scales: 50% to 300% in steps of 5%, on
 * every device, well beyond what most devices' settings offer.
 */
internal val FontScaleSteps: List<ScaleStep> = (50..300 step 5).map { percent ->
    ScaleStep(if (percent == 100) DefaultLabel else "$percent%", percent / 100f)
}

/**
 * The display sizes the device offers, smallest first, as Android's settings work them out: one
 * size smaller than the default and up to three larger, the largest leaving the screen's shorter
 * side 320 dp, at most 1.5 times the default.
 */
internal val Device.displayScaleSteps: List<ScaleStep>
    get() {
        val shorterSide = portraitScreenSize.let { min(it.width, it.height) }
        val maxScale = min(MaxDisplayScale, shorterSide / MinShorterSide)
        val larger = ((maxScale - 1f) / MinDisplayScaleInterval).toInt().coerceIn(0, LargerDisplayLabels.size)
        return buildList {
            add(ScaleStep("Small", MinDisplayScale))
            add(ScaleStep(DefaultLabel, 1f))
            for (step in 1..larger) {
                add(ScaleStep(LargerDisplayLabels[step - 1], 1f + step * (maxScale - 1f) / larger))
            }
        }
    }

private const val DefaultLabel = "Default"
private const val MinDisplayScale = 0.85f
private const val MaxDisplayScale = 1.5f
private const val MinDisplayScaleInterval = 0.09f
private const val MinShorterSide = 320f
private val LargerDisplayLabels = listOf("Large", "Larger", "Largest")

/** The step nearest [scale], or of two as near, the one nearer the default. */
internal fun List<ScaleStep>.nearest(scale: Float): ScaleStep {
    val distance = minOf { abs(it.scale - scale) }
    return filter { abs(it.scale - scale) <= distance + 0.001f }.minBy { abs(it.scale - 1f) }
}
