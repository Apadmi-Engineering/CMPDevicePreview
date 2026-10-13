package com.apadmi.cmpdevicepreview.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Explore, arranged for each layout.
 *
 * @param state The app state that supplies the search query, category filter and results.
 * @param layout The layout to arrange the screen for.
 * @param modifier The modifier for the screen.
 */
@Composable
fun ExploreScreen(
    state: WanderState,
    layout: AppLayout,
    modifier: Modifier = Modifier
) {
    when (layout) {
        AppLayout.Phone -> PhoneExplore(state, modifier)
        AppLayout.PhoneLandscape -> LandscapeExplore(state, modifier)
        AppLayout.Tablet, AppLayout.Desktop, AppLayout.LargeDesktop -> GridExplore(state, layout, modifier)
    }
}

@Composable
private fun ExploreHeader(
    state: WanderState,
    layout: AppLayout,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BreakpointBadge(layout)
        Column {
            Text(
                "Where to next?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${destinations.size} places to explore",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SearchField(state.query, { state.query = it })
    }
}

@Composable
private fun NoResults(state: WanderState, modifier: Modifier = Modifier) {
    EmptyState(Icons.Rounded.SearchOff, "No destinations found", "Nothing matches \"${state.query}\".", modifier)
}

/** A single column: a featured card, then a list. */
@Composable
private fun PhoneExplore(state: WanderState, modifier: Modifier) {
    val results = state.results
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ExploreHeader(state, AppLayout.Phone) }
        item { CategoryChips(state.category, { state.category = it }) }
        val featured = results.firstOrNull()
        featured?.let {
            item { FeaturedCard(featured, state, Modifier.fillMaxWidth().height(300.dp)) }
        } ?: item { NoResults(state, Modifier.fillMaxWidth()) }
        if (results.size > 1) {
            item { Text("Popular", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        }
        items(results.drop(1), key = { it.id }) { destination -> ListCard(destination, state) }
    }
}

/** Side by side, for the short screen of a phone turned sideways: the featured card beside a grid. */
@Composable
private fun LandscapeExplore(state: WanderState, modifier: Modifier) {
    val results = state.results
    Row(modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier.weight(0.42f).fillMaxHeight().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SearchField(state.query, { state.query = it })
            val featured = results.firstOrNull()
            featured?.let {
                FeaturedCard(featured, state, Modifier.weight(1f).fillMaxWidth())
            } ?: NoResults(state, Modifier.fillMaxWidth())
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(0.58f).fillMaxHeight(),
            contentPadding = PaddingValues(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BreakpointBadge(AppLayout.PhoneLandscape)
                    CategoryChips(state.category, { state.category = it }, wrap = true)
                }
            }
            items(results.drop(1), key = { it.id }) { destination ->
                TileCard(destination, state, artAspectRatio = 16f / 9f)
            }
        }
    }
}

/**
 * A grid with as many columns as fit. Without a detail pane the first result is a wide banner;
 * with one, which shows the selection, every result is a card.
 */
@Composable
private fun GridExplore(
    state: WanderState,
    layout: AppLayout,
    modifier: Modifier
) {
    val results = state.results
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { ExploreHeader(state, layout) }
        item(span = { GridItemSpan(maxLineSpan) }) { CategoryChips(state.category, { state.category = it }, wrap = true) }
        if (results.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { NoResults(state) }
        }
        val featured = results.firstOrNull().takeUnless { layout.hasDetailPane }
        featured?.let {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FeaturedCard(featured, state, Modifier.fillMaxWidth().height(280.dp))
            }
        }
        items(featured?.let {
            results.drop(1)
        } ?: results, key = { it.id }) { destination ->
            TileCard(destination, state, isSelected = layout.hasDetailPane && destination == state.selected)
        }
    }
}
