package com.kappmaker.sample

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

/** Explore, arranged for each layout. */
@Composable
fun ExploreScreen(state: WanderState, layout: AppLayout, modifier: Modifier = Modifier) {
    when (layout) {
        AppLayout.Phone -> PhoneExplore(state, modifier)
        AppLayout.PhoneLandscape -> LandscapeExplore(state, modifier)
        AppLayout.Tablet -> GridExplore(state, layout, modifier, showFeatured = true)
        AppLayout.Desktop, AppLayout.LargeDesktop -> GridExplore(state, layout, modifier, showFeatured = false)
    }
}

@Composable
private fun ExploreHeader(state: WanderState, layout: AppLayout, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BreakpointBadge(layout)
        Column {
            Text("Where to next?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
        if (featured == null) {
            item { NoResults(state, Modifier.fillMaxWidth()) }
        } else {
            item {
                FeaturedCard(
                    featured, featured.id in state.saved, { state.toggleSaved(featured) }, { state.open(featured) },
                    Modifier.fillMaxWidth().height(300.dp),
                )
            }
        }
        if (results.size > 1) {
            item { Text("Popular", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        }
        items(results.drop(1), key = { it.id }) { destination ->
            ListCard(destination, destination.id in state.saved, { state.toggleSaved(destination) }, { state.open(destination) })
        }
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
            if (featured == null) {
                NoResults(state, Modifier.fillMaxWidth())
            } else {
                FeaturedCard(
                    featured, featured.id in state.saved, { state.toggleSaved(featured) }, { state.open(featured) },
                    Modifier.weight(1f).fillMaxWidth(),
                )
            }
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
                TileCard(
                    destination, destination.id in state.saved, { state.toggleSaved(destination) }, { state.open(destination) },
                    artAspectRatio = 16f / 9f,
                )
            }
        }
    }
}

/**
 * A grid with as many columns as fit. With [showFeatured] the first result is a wide banner;
 * without it (the desktop, whose detail pane shows the selection) every result is a card.
 */
@Composable
private fun GridExplore(state: WanderState, layout: AppLayout, modifier: Modifier, showFeatured: Boolean) {
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
        val featured = results.firstOrNull().takeIf { showFeatured }
        if (featured != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FeaturedCard(
                    featured, featured.id in state.saved, { state.toggleSaved(featured) }, { state.open(featured) },
                    Modifier.fillMaxWidth().height(280.dp),
                )
            }
        }
        items(if (featured != null) results.drop(1) else results, key = { it.id }) { destination ->
            TileCard(
                destination, destination.id in state.saved, { state.toggleSaved(destination) }, { state.open(destination) },
                isSelected = !showFeatured && destination == state.selected,
            )
        }
    }
}
