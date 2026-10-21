package com.apadmi.cmpdevicepreview.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apadmi.cmpdevicepreview.sample.resources.Res
import com.apadmi.cmpdevicepreview.sample.resources.breakpoint_badge
import com.apadmi.cmpdevicepreview.sample.resources.button_save
import com.apadmi.cmpdevicepreview.sample.resources.category_all
import com.apadmi.cmpdevicepreview.sample.resources.featured
import com.apadmi.cmpdevicepreview.sample.resources.nights
import com.apadmi.cmpdevicepreview.sample.resources.price_from
import com.apadmi.cmpdevicepreview.sample.resources.save_remove
import com.apadmi.cmpdevicepreview.sample.resources.search_clear
import com.apadmi.cmpdevicepreview.sample.resources.search_placeholder
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

val Category.icon: ImageVector
    get() = when (this) {
        Category.Mountains -> Icons.Rounded.Landscape
        Category.Coast -> Icons.Rounded.BeachAccess
        Category.City -> Icons.Rounded.LocationCity
        Category.Desert -> Icons.Rounded.WbSunny
    }

@Composable
fun AppMark(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(40.dp)
            .background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)),
                RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.TravelExplore, contentDescription = null, tint = Color.White)
    }
}

/**
 * Which layout the app picked, and the window size that picked it.
 *
 * @param layout The layout whose label the badge shows.
 * @param modifier The modifier for the badge.
 */
@Composable
fun BreakpointBadge(layout: AppLayout, modifier: Modifier = Modifier) {
    val size = currentWindowSize()
    Surface(modifier, shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Devices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Text(
                stringResource(
                    Res.string.breakpoint_badge,
                    stringResource(layout.label),
                    size.width.value.roundToInt(),
                    size.height.value.roundToInt(),
                ),
                modifier = Modifier.padding(start = 6.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(Res.string.search_placeholder)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.search_clear))
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

/**
 * "All" and each category; a scrolling row, or wrapped onto as many lines as it needs.
 *
 * @param selected The selected category, or null when "All" is selected.
 * @param onSelect Called with the category of the chip that is tapped, or null for "All".
 * @param modifier The modifier for the row of chips.
 * @param wrap Whether the chips wrap onto as many lines as they need, rather than scrolling
 * sideways in a single row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    selected: Category?,
    onSelect: (Category?) -> Unit,
    modifier: Modifier = Modifier,
    wrap: Boolean = false,
) {
    val chips: @Composable () -> Unit = {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(Res.string.category_all)) },
            leadingIcon = { Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(18.dp)) },
            // The default hover elevation casts a shadow that shows through an unselected chip's transparent container.
            elevation = null,
        )
        for (category in Category.entries) {
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(stringResource(category.label)) },
                leadingIcon = { Icon(category.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                elevation = null,
            )
        }
    }
    if (wrap) {
        FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
    } else {
        Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
    }
}

@Composable
fun SaveButton(
    destination: Destination,
    state: WanderState,
    modifier: Modifier = Modifier,
    onArt: Boolean = false
) {
    val isSaved = destination.id in state.saved
    IconButton(
        onClick = { state.toggleSaved(destination) },
        modifier = modifier,
        colors = if (onArt) {
            IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f), contentColor = Color.White)
        } else {
            IconButtonDefaults.iconButtonColors()
        },
    ) {
        Icon(
            if (isSaved) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(if (isSaved) Res.string.save_remove else Res.string.button_save),
            tint = if (isSaved) {
                Color(0xFFFF5A79)
            } else if (onArt) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
fun RatingRow(
    destination: Destination,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val nights = pluralStringResource(Res.plurals.nights, destination.nights, destination.nights)
    val price = stringResource(Res.string.price_from, destination.price)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = RatingColor, modifier = Modifier.size(16.dp))
        Text(
            "${destination.rating} · $nights · $price",
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A large card with the destination's name over its illustration.
 *
 * @param destination The destination the card shows.
 * @param state The app state that tapping the card opens the destination in, and that the card's
 * save button updates.
 * @param modifier The modifier for the card.
 */
@Composable
fun FeaturedCard(
    destination: Destination,
    state: WanderState,
    modifier: Modifier = Modifier
) {
    Box(modifier.clip(RoundedCornerShape(28.dp)).clickable { state.open(destination) }) {
        SceneryArt(destination, Modifier.matchParentSize())
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f)))
        )
        SaveButton(destination, state, Modifier.align(Alignment.TopEnd).padding(12.dp), onArt = true)
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text(
                stringResource(Res.string.featured),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )
            Text(
                stringResource(destination.name),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Place, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                Text(stringResource(destination.country), color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(start = 4.dp))
            }
            RatingRow(destination, Modifier.padding(top = 4.dp), color = Color.White.copy(alpha = 0.85f))
        }
    }
}

/**
 * A card for a grid: the illustration above the name.
 *
 * @param destination The destination the card shows.
 * @param state The app state that tapping the card opens the destination in, and that the card's
 * save button updates.
 * @param modifier The modifier for the card.
 * @param isSelected Whether the card is highlighted as the destination shown in the detail pane.
 * @param artAspectRatio The width-to-height ratio of the illustration.
 */
@Composable
fun TileCard(
    destination: Destination,
    state: WanderState,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    artAspectRatio: Float = 4f / 3f,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) colors.primaryContainer else colors.surfaceContainerLow)
            .clickable { state.open(destination) }
            .padding(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(artAspectRatio).clip(RoundedCornerShape(18.dp))) {
            SceneryArt(destination, Modifier.matchParentSize())
            SaveButton(destination, state, Modifier.align(Alignment.TopEnd).padding(4.dp), onArt = true)
        }
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
            Text(
                stringResource(destination.name),
                color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(stringResource(destination.country), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            RatingRow(destination, Modifier.padding(top = 4.dp))
        }
    }
}

/**
 * A card for a list: a thumbnail beside the name.
 *
 * @param destination The destination the card shows.
 * @param state The app state that tapping the card opens the destination in, and that the card's
 * save button updates.
 * @param modifier The modifier for the card.
 */
@Composable
fun ListCard(
    destination: Destination,
    state: WanderState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { state.open(destination) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SceneryArt(destination, Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                stringResource(destination.name),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(destination.country),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RatingRow(destination, Modifier.padding(top = 4.dp))
        }
        SaveButton(destination, state)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
