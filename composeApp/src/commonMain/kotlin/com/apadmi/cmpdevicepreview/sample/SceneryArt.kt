package com.apadmi.cmpdevicepreview.sample

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import kotlin.random.Random

/**
 * A landscape drawn from the destination's colours: a sky, a sun and three layers of land, fading
 * into the distance. The same destination always draws the same landscape.
 *
 * @param destination The destination whose sky, land, sun and scene the landscape is drawn from.
 * @param modifier The modifier for the illustration, which sets the size it is drawn at.
 */
@Composable
fun SceneryArt(destination: Destination, modifier: Modifier = Modifier) {
    Spacer(
        modifier.drawWithCache {
            val sky = Brush.verticalGradient(destination.sky)
            val random = Random(destination.id)
            val land = (0 until 3).map { layer ->
                val color = lerp(destination.sky.last(), destination.land, (layer + 1) / 3f)
                val baseline = size.height * (0.5f + layer * 0.16f)
                landPath(destination.scene, size, baseline, layer, random) to color
            }
            onDrawBehind {
                drawRect(sky)
                drawCircle(
                    color = destination.sun,
                    radius = size.minDimension * 0.11f,
                    center = Offset(size.width * 0.7f, size.height * 0.3f),
                )
                for ((path, color) in land) drawPath(path, color)
            }
        }
    )
}

private fun landPath(scene: Scene, size: Size, baseline: Float, layer: Int, random: Random): Path = Path().apply {
    val width = size.width
    val height = size.height
    val swing = height * (0.18f - layer * 0.04f)
    moveTo(0f, height)
    when (scene) {
        Scene.Peaks -> {
            val peaks = 4 + layer
            lineTo(0f, baseline)
            for (i in 0 until peaks) {
                val left = width * i / peaks
                val step = width / peaks
                lineTo(left + step * (0.3f + random.nextFloat() * 0.4f), baseline - swing * (0.6f + random.nextFloat()))
                lineTo(left + step, baseline - swing * random.nextFloat() * 0.3f)
            }
        }
        Scene.Hills, Scene.Dunes -> {
            val humps = if (scene == Scene.Dunes) 2 else 3
            lineTo(0f, baseline)
            for (i in 0 until humps) {
                val left = width * i / humps
                val step = width / humps
                val top = baseline - swing * (0.4f + random.nextFloat() * 0.6f)
                quadraticTo(left + step / 2f, top - swing * 0.6f, left + step, baseline - swing * random.nextFloat() * 0.3f)
            }
        }
        Scene.Sea -> {
            if (layer == 0) {
                // A headland on the horizon.
                lineTo(0f, baseline - swing * 0.5f)
                quadraticTo(width * 0.2f, baseline - swing * 1.4f, width * 0.45f, baseline)
                lineTo(width, baseline)
            } else {
                val waves = 6 + layer * 2
                lineTo(0f, baseline)
                for (i in 0 until waves) {
                    val step = width / waves
                    quadraticTo(step * i + step / 2f, baseline - swing * 0.25f, step * (i + 1), baseline)
                }
            }
        }
        Scene.Skyline -> {
            if (layer == 2) {
                lineTo(0f, baseline + swing)
                lineTo(width, baseline + swing)
            } else {
                lineTo(0f, baseline)
                var x = 0f
                while (x < width) {
                    val buildingWidth = width * (0.05f + random.nextFloat() * 0.07f)
                    val top = baseline - swing * (0.3f + random.nextFloat() * 1.2f)
                    lineTo(x, top)
                    lineTo(x + buildingWidth, top)
                    x += buildingWidth
                    lineTo(x, baseline)
                }
            }
        }
    }
    lineTo(width, height)
    close()
}
