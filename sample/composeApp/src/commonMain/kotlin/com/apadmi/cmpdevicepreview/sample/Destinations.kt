package com.apadmi.cmpdevicepreview.sample

import androidx.compose.ui.graphics.Color
import com.apadmi.cmpdevicepreview.sample.resources.Res
import com.apadmi.cmpdevicepreview.sample.resources.category_city
import com.apadmi.cmpdevicepreview.sample.resources.category_coast
import com.apadmi.cmpdevicepreview.sample.resources.category_desert
import com.apadmi.cmpdevicepreview.sample.resources.category_mountains
import com.apadmi.cmpdevicepreview.sample.resources.destination_amalfi_coast_blurb
import com.apadmi.cmpdevicepreview.sample.resources.destination_amalfi_coast_country
import com.apadmi.cmpdevicepreview.sample.resources.destination_amalfi_coast_name
import com.apadmi.cmpdevicepreview.sample.resources.destination_banff_blurb
import com.apadmi.cmpdevicepreview.sample.resources.destination_banff_country
import com.apadmi.cmpdevicepreview.sample.resources.destination_banff_name
import com.apadmi.cmpdevicepreview.sample.resources.destination_cape_town_blurb
import com.apadmi.cmpdevicepreview.sample.resources.destination_cape_town_country
import com.apadmi.cmpdevicepreview.sample.resources.destination_cape_town_name
import com.apadmi.cmpdevicepreview.sample.resources.destination_hoi_an_blurb
import com.apadmi.cmpdevicepreview.sample.resources.destination_hoi_an_country
import com.apadmi.cmpdevicepreview.sample.resources.destination_hoi_an_name
import com.apadmi.cmpdevicepreview.sample.resources.destination_kyoto_blurb
import com.apadmi.cmpdevicepreview.sample.resources.destination_kyoto_country
import com.apadmi.cmpdevicepreview.sample.resources.destination_kyoto_name
import com.apadmi.cmpdevicepreview.sample.resources.destination_lisbon_blurb
import com.apadmi.cmpdevicepreview.sample.resources.destination_lisbon_country
import com.apadmi.cmpdevicepreview.sample.resources.destination_lisbon_name
import com.apadmi.cmpdevicepreview.sample.resources.destination_lofoten_blurb
import com.apadmi.cmpdevicepreview.sample.resources.destination_lofoten_country
import com.apadmi.cmpdevicepreview.sample.resources.destination_lofoten_name
import com.apadmi.cmpdevicepreview.sample.resources.destination_merzouga_blurb
import com.apadmi.cmpdevicepreview.sample.resources.destination_merzouga_country
import com.apadmi.cmpdevicepreview.sample.resources.destination_merzouga_name
import com.apadmi.cmpdevicepreview.sample.resources.destination_patagonia_blurb
import com.apadmi.cmpdevicepreview.sample.resources.destination_patagonia_country
import com.apadmi.cmpdevicepreview.sample.resources.destination_patagonia_name
import com.apadmi.cmpdevicepreview.sample.resources.destination_santorini_blurb
import com.apadmi.cmpdevicepreview.sample.resources.destination_santorini_country
import com.apadmi.cmpdevicepreview.sample.resources.destination_santorini_name
import com.apadmi.cmpdevicepreview.sample.resources.destination_thingvellir_blurb
import com.apadmi.cmpdevicepreview.sample.resources.destination_thingvellir_country
import com.apadmi.cmpdevicepreview.sample.resources.destination_thingvellir_name
import com.apadmi.cmpdevicepreview.sample.resources.destination_wadi_rum_blurb
import com.apadmi.cmpdevicepreview.sample.resources.destination_wadi_rum_country
import com.apadmi.cmpdevicepreview.sample.resources.destination_wadi_rum_name
import org.jetbrains.compose.resources.StringResource

val destinations: List<Destination> = listOf(
    Destination(
        id = 1, name = Res.string.destination_lofoten_name, country = Res.string.destination_lofoten_country, category = Category.Mountains,
        rating = 4.9f, nights = 5, price = 840,
        blurb = Res.string.destination_lofoten_blurb,
        sky = listOf(Color(0xFF3B2A6B), Color(0xFFE88BA6)), land = Color(0xFF1C1633),
        sun = Color(0xFFFFD3B0), scene = Scene.Peaks,
    ),
    Destination(
        id = 2, name = Res.string.destination_santorini_name, country = Res.string.destination_santorini_country, category = Category.Coast,
        rating = 4.8f, nights = 4, price = 720,
        blurb = Res.string.destination_santorini_blurb,
        sky = listOf(Color(0xFF2B79C2), Color(0xFFA6E1FA)), land = Color(0xFF0B3D6B),
        sun = Color(0xFFFFF4C2), scene = Scene.Sea,
    ),
    Destination(
        id = 3, name = Res.string.destination_kyoto_name, country = Res.string.destination_kyoto_country, category = Category.City,
        rating = 4.9f, nights = 6, price = 1150,
        blurb = Res.string.destination_kyoto_blurb,
        sky = listOf(Color(0xFFF2994A), Color(0xFFF9D976)), land = Color(0xFF5B2333),
        sun = Color(0xFFFFF1D6), scene = Scene.Skyline,
    ),
    Destination(
        id = 4, name = Res.string.destination_patagonia_name, country = Res.string.destination_patagonia_country, category = Category.Mountains,
        rating = 4.7f, nights = 8, price = 1480,
        blurb = Res.string.destination_patagonia_blurb,
        sky = listOf(Color(0xFF1D4E89), Color(0xFF7FB3D5)), land = Color(0xFF0E2238),
        sun = Color(0xFFEAF6FF), scene = Scene.Peaks,
    ),
    Destination(
        id = 5, name = Res.string.destination_merzouga_name, country = Res.string.destination_merzouga_country, category = Category.Desert,
        rating = 4.6f, nights = 3, price = 460,
        blurb = Res.string.destination_merzouga_blurb,
        sky = listOf(Color(0xFFE96443), Color(0xFFFFC371)), land = Color(0xFF8A3B12),
        sun = Color(0xFFFFF0C9), scene = Scene.Dunes,
    ),
    Destination(
        id = 6, name = Res.string.destination_banff_name, country = Res.string.destination_banff_country, category = Category.Mountains,
        rating = 4.8f, nights = 5, price = 990,
        blurb = Res.string.destination_banff_blurb,
        sky = listOf(Color(0xFF136A8A), Color(0xFF8FD3C7)), land = Color(0xFF0B3B3C),
        sun = Color(0xFFF4FFF8), scene = Scene.Peaks,
    ),
    Destination(
        id = 7, name = Res.string.destination_amalfi_coast_name, country = Res.string.destination_amalfi_coast_country, category = Category.Coast,
        rating = 4.8f, nights = 5, price = 1020,
        blurb = Res.string.destination_amalfi_coast_blurb,
        sky = listOf(Color(0xFFF6A35C), Color(0xFFFFE1A8)), land = Color(0xFF1F5C7A),
        sun = Color(0xFFFFF6E0), scene = Scene.Sea,
    ),
    Destination(
        id = 8, name = Res.string.destination_thingvellir_name, country = Res.string.destination_thingvellir_country, category = Category.Mountains,
        rating = 4.7f, nights = 4, price = 880,
        blurb = Res.string.destination_thingvellir_blurb,
        sky = listOf(Color(0xFF0B1D3A), Color(0xFF2E8B6F)), land = Color(0xFF081325),
        sun = Color(0xFFD9FFE9), scene = Scene.Hills,
    ),
    Destination(
        id = 9, name = Res.string.destination_lisbon_name, country = Res.string.destination_lisbon_country, category = Category.City,
        rating = 4.7f, nights = 4, price = 640,
        blurb = Res.string.destination_lisbon_blurb,
        sky = listOf(Color(0xFF56CCF2), Color(0xFFFFF3B0)), land = Color(0xFF7A3E1D),
        sun = Color(0xFFFFFFFF), scene = Scene.Skyline,
    ),
    Destination(
        id = 10, name = Res.string.destination_wadi_rum_name, country = Res.string.destination_wadi_rum_country, category = Category.Desert,
        rating = 4.8f, nights = 3, price = 540,
        blurb = Res.string.destination_wadi_rum_blurb,
        sky = listOf(Color(0xFF7B4397), Color(0xFFDC2430)), land = Color(0xFF4A1414),
        sun = Color(0xFFFFD6A5), scene = Scene.Dunes,
    ),
    Destination(
        id = 11, name = Res.string.destination_cape_town_name, country = Res.string.destination_cape_town_country, category = Category.Coast,
        rating = 4.7f, nights = 6, price = 930,
        blurb = Res.string.destination_cape_town_blurb,
        sky = listOf(Color(0xFF4568DC), Color(0xFFB06AB3)), land = Color(0xFF1B1F4B),
        sun = Color(0xFFFFE4F3), scene = Scene.Hills,
    ),
    Destination(
        id = 12, name = Res.string.destination_hoi_an_name, country = Res.string.destination_hoi_an_country, category = Category.City,
        rating = 4.8f, nights = 4, price = 590,
        blurb = Res.string.destination_hoi_an_blurb,
        sky = listOf(Color(0xFF8E2DE2), Color(0xFFF7797D)), land = Color(0xFF2E0F3A),
        sun = Color(0xFFFFE0B5), scene = Scene.Skyline,
    ),
)

/**
 * What kind of place a destination is.
 *
 * @property label The name shown for the category.
 */
enum class Category(val label: StringResource) {
    Mountains(Res.string.category_mountains),
    Coast(Res.string.category_coast),
    City(Res.string.category_city),
    Desert(Res.string.category_desert),
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
    val name: StringResource,
    val country: StringResource,
    val category: Category,
    val rating: Float,
    val nights: Int,
    val price: Int,
    val blurb: StringResource,
    val sky: List<Color>,
    val land: Color,
    val sun: Color,
    val scene: Scene,
)
