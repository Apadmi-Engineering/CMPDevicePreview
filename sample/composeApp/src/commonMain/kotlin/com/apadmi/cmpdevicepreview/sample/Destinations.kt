package com.apadmi.cmpdevicepreview.sample

import androidx.compose.ui.graphics.Color

val destinations: List<Destination> = listOf(
    Destination(
        id = 1, name = "Lofoten", country = "Norway", category = Category.Mountains,
        rating = 4.9f, nights = 5, price = 840,
        blurb = "Granite peaks rise straight out of the sea, with red fishing cabins along the shore " +
                "and the midnight sun in summer.",
        sky = listOf(Color(0xFF3B2A6B), Color(0xFFE88BA6)), land = Color(0xFF1C1633),
        sun = Color(0xFFFFD3B0), scene = Scene.Peaks,
    ),
    Destination(
        id = 2, name = "Santorini", country = "Greece", category = Category.Coast,
        rating = 4.8f, nights = 4, price = 720,
        blurb = "Whitewashed villages on the rim of a flooded caldera, and sunsets over the Aegean " +
                "from Oia's terraces.",
        sky = listOf(Color(0xFF2B79C2), Color(0xFFA6E1FA)), land = Color(0xFF0B3D6B),
        sun = Color(0xFFFFF4C2), scene = Scene.Sea,
    ),
    Destination(
        id = 3, name = "Kyoto", country = "Japan", category = Category.City,
        rating = 4.9f, nights = 6, price = 1150,
        blurb = "Temples, tea houses and moss gardens, with lantern-lit lanes in Gion after dark.",
        sky = listOf(Color(0xFFF2994A), Color(0xFFF9D976)), land = Color(0xFF5B2333),
        sun = Color(0xFFFFF1D6), scene = Scene.Skyline,
    ),
    Destination(
        id = 4, name = "Patagonia", country = "Argentina", category = Category.Mountains,
        rating = 4.7f, nights = 8, price = 1480,
        blurb = "Glaciers, turquoise lakes and the granite towers of Fitz Roy at the end of the world.",
        sky = listOf(Color(0xFF1D4E89), Color(0xFF7FB3D5)), land = Color(0xFF0E2238),
        sun = Color(0xFFEAF6FF), scene = Scene.Peaks,
    ),
    Destination(
        id = 5, name = "Merzouga", country = "Morocco", category = Category.Desert,
        rating = 4.6f, nights = 3, price = 460,
        blurb = "The dunes of Erg Chebbi glow orange at dawn; nights in a camp under a sky full of stars.",
        sky = listOf(Color(0xFFE96443), Color(0xFFFFC371)), land = Color(0xFF8A3B12),
        sun = Color(0xFFFFF0C9), scene = Scene.Dunes,
    ),
    Destination(
        id = 6, name = "Banff", country = "Canada", category = Category.Mountains,
        rating = 4.8f, nights = 5, price = 990,
        blurb = "Glacier-fed lakes the colour of jade beneath the Canadian Rockies.",
        sky = listOf(Color(0xFF136A8A), Color(0xFF8FD3C7)), land = Color(0xFF0B3B3C),
        sun = Color(0xFFF4FFF8), scene = Scene.Peaks,
    ),
    Destination(
        id = 7, name = "Amalfi Coast", country = "Italy", category = Category.Coast,
        rating = 4.8f, nights = 5, price = 1020,
        blurb = "Pastel towns stacked on cliffs above the Tyrrhenian Sea, linked by a winding coast road.",
        sky = listOf(Color(0xFFF6A35C), Color(0xFFFFE1A8)), land = Color(0xFF1F5C7A),
        sun = Color(0xFFFFF6E0), scene = Scene.Sea,
    ),
    Destination(
        id = 8, name = "Þingvellir", country = "Iceland", category = Category.Mountains,
        rating = 4.7f, nights = 4, price = 880,
        blurb = "A rift valley between two continents, with the northern lights overhead in winter.",
        sky = listOf(Color(0xFF0B1D3A), Color(0xFF2E8B6F)), land = Color(0xFF081325),
        sun = Color(0xFFD9FFE9), scene = Scene.Hills,
    ),
    Destination(
        id = 9, name = "Lisbon", country = "Portugal", category = Category.City,
        rating = 4.7f, nights = 4, price = 640,
        blurb = "Yellow trams climb tiled hillsides to viewpoints over the Tagus.",
        sky = listOf(Color(0xFF56CCF2), Color(0xFFFFF3B0)), land = Color(0xFF7A3E1D),
        sun = Color(0xFFFFFFFF), scene = Scene.Skyline,
    ),
    Destination(
        id = 10, name = "Wadi Rum", country = "Jordan", category = Category.Desert,
        rating = 4.8f, nights = 3, price = 540,
        blurb = "Red sand and sandstone mountains, the Valley of the Moon, best seen by jeep at sunset.",
        sky = listOf(Color(0xFF7B4397), Color(0xFFDC2430)), land = Color(0xFF4A1414),
        sun = Color(0xFFFFD6A5), scene = Scene.Dunes,
    ),
    Destination(
        id = 11, name = "Cape Town", country = "South Africa", category = Category.Coast,
        rating = 4.7f, nights = 6, price = 930,
        blurb = "Table Mountain above two oceans, with penguins on Boulders Beach.",
        sky = listOf(Color(0xFF4568DC), Color(0xFFB06AB3)), land = Color(0xFF1B1F4B),
        sun = Color(0xFFFFE4F3), scene = Scene.Hills,
    ),
    Destination(
        id = 12, name = "Hội An", country = "Vietnam", category = Category.City,
        rating = 4.8f, nights = 4, price = 590,
        blurb = "Silk lanterns over the Thu Bồn river in an old trading port of yellow shophouses.",
        sky = listOf(Color(0xFF8E2DE2), Color(0xFFF7797D)), land = Color(0xFF2E0F3A),
        sun = Color(0xFFFFE0B5), scene = Scene.Skyline,
    ),
)

/**
 * What kind of place a destination is.
 *
 * @property label The name shown for the category.
 */
enum class Category(val label: String) {
    Mountains("Mountains"),
    Coast("Coast"),
    City("City"),
    Desert("Desert"),
    ;
}

/** The kind of landscape a destination's illustration draws. */
enum class Scene {
    Peaks, Hills, Sea, Dunes, Skyline
}

/**
 * A place the sample app offers, with the colours its illustration is drawn from.
 *
 * @property id Tells destinations apart, and seeds its illustration so it always draws the same.
 * @property name The destination's name.
 * @property country The country it is in.
 * @property category What kind of place it is.
 * @property rating The average review score.
 * @property nights How many nights a stay is priced for.
 * @property price What the stay costs, in pounds, from.
 * @property blurb A short description.
 * @property sky The sky, top to bottom.
 * @property land The nearest layer of land; the layers behind it fade towards the sky.
 * @property sun The colour of the sun.
 * @property scene The kind of landscape its illustration draws.
 */
data class Destination(
    val id: Int,
    val name: String,
    val country: String,
    val category: Category,
    val rating: Float,
    val nights: Int,
    val price: Int,
    val blurb: String,
    val sky: List<Color>,
    val land: Color,
    val sun: Color,
    val scene: Scene,
)
