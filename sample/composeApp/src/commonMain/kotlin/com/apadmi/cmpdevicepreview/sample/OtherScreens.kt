package com.apadmi.cmpdevicepreview.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The saved destinations: a list on a phone, a grid with more room.
 *
 * @param state The app state whose saved destinations the screen shows.
 * @param layout The layout to arrange the screen for: a list for [AppLayout.Phone], a grid for the
 * others.
 * @param modifier The modifier for the screen.
 */
@Composable
fun SavedScreen(
    state: WanderState,
    layout: AppLayout,
    modifier: Modifier = Modifier
) {
    val saved = destinations.filter { it.id in state.saved }
    val title: @Composable () -> Unit = {
        Column {
            Text("Saved", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "${saved.size} ${if (saved.size == 1) "place" else "places"} on your list",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val empty: @Composable () -> Unit = {
        EmptyState(
            Icons.Outlined.FavoriteBorder, "Nothing saved yet", "Tap the heart on a destination to keep it here.",
            Modifier.fillMaxWidth(),
        )
    }
    if (layout == AppLayout.Phone) {
        LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { title() }
            if (saved.isEmpty()) {
                item { empty() }
            }
            items(saved, key = { it.id }) { destination -> ListCard(destination, state) }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { title() }
            if (saved.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { empty() }
            }
            items(saved, key = { it.id }) { destination ->
                TileCard(
                    destination, state,
                    isSelected = layout.hasDetailPane && destination == state.selected,
                    artAspectRatio = if (layout == AppLayout.PhoneLandscape) 16f / 9f else 4f / 3f,
                )
            }
        }
    }
}

/**
 * A profile and a few settings, in a column that stops growing on wide screens.
 *
 * @param state The app state that supplies the number of saved destinations in the profile stats.
 * @param modifier The modifier for the screen.
 */
@Composable
fun ProfileScreen(state: WanderState, modifier: Modifier = Modifier) {
    var reminders by remember { mutableStateOf(true) }
    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("AR", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text("Alex Rivera", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Exploring since 2021", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("14", "Trips", Modifier.weight(1f))
                Stat("9", "Countries", Modifier.weight(1f))
                Stat("${state.saved.size}", "Saved", Modifier.weight(1f))
            }
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column {
                    SettingRow(Icons.Rounded.NotificationsActive, "Trip reminders") {
                        Switch(checked = reminders, onCheckedChange = { reminders = it })
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Rounded.Payments, "Currency") { SettingValue("GBP") }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Rounded.Language, "Language") { SettingValue("English") }
                }
            }
        }
    }
}

@Composable
private fun Stat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    trailing: @Composable () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyLarge)
        trailing()
    }
}

@Composable
private fun SettingValue(value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
