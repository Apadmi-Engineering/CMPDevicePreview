package com.apadmi.cmpdevicepreview.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Explore("Explore", Icons.Outlined.Explore, Icons.Rounded.Explore),
    Saved("Saved", Icons.Outlined.FavoriteBorder, Icons.Rounded.Favorite),
    Profile("Profile", Icons.Outlined.Person, Icons.Rounded.Person),
}

class WanderState {
    var tab by mutableStateOf(Tab.Explore)
    var query by mutableStateOf("")
    var category by mutableStateOf<Category?>(null)
    var saved by mutableStateOf(setOf(1, 3, 7))

    /** The destination in the detail pane, on the layouts that have one. */
    var selected by mutableStateOf(destinations.first())

    /** The destination shown in place of the tab, on the layouts without a detail pane. */
    var opened by mutableStateOf<Destination?>(null)

    val results: List<Destination>
        get() = destinations.filter { destination ->
            (category == null || destination.category == category) &&
                (query.isBlank() || listOf(destination.name, destination.country).any { it.contains(query.trim(), ignoreCase = true) })
        }

    fun toggleSaved(destination: Destination) {
        saved = if (destination.id in saved) saved - destination.id else saved + destination.id
    }

    /** Shows [destination]: in the detail pane, or, without one, in place of the tab. */
    fun open(destination: Destination) {
        selected = destination
        opened = destination
    }

    fun showTab(tab: Tab) {
        this.tab = tab
        opened = null
    }
}

@Composable
@Preview
fun App() {
    WanderTheme {
        val state = remember { WanderState() }
        when (val layout = currentAppLayout()) {
            AppLayout.Phone -> PhoneApp(state)
            AppLayout.PhoneLandscape, AppLayout.Tablet, AppLayout.Desktop -> RailApp(state, layout)
            AppLayout.LargeDesktop -> DrawerApp(state)
        }
    }
}

@Composable
private fun TabContent(state: WanderState, layout: AppLayout, modifier: Modifier = Modifier) {
    when (state.tab) {
        Tab.Explore -> ExploreScreen(state, layout, modifier)
        Tab.Saved -> SavedScreen(state, layout, modifier)
        Tab.Profile -> ProfileScreen(state, modifier)
    }
}

/** The tab, with the layout's detail pane beside it when there's a destination to show. */
@Composable
private fun TabWithDetail(state: WanderState, layout: AppLayout, modifier: Modifier = Modifier) {
    Row(modifier) {
        TabContent(state, layout, Modifier.weight(1f))
        val detailPaneWidth = layout.detailPaneWidth
        if (detailPaneWidth != null && state.tab != Tab.Profile) {
            DetailPane(state, Modifier.width(detailPaneWidth))
        }
    }
}

/** A bottom navigation bar; a destination opens full screen, edge to edge. */
@Composable
private fun PhoneApp(state: WanderState) {
    val opened = state.opened
    if (opened != null) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DestinationDetail(opened, state, AppLayout.Phone, onBack = { state.opened = null })
        }
        return
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                for (tab in Tab.entries) {
                    val selected = state.tab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { state.showTab(tab) },
                        icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        TabContent(state, AppLayout.Phone, Modifier.padding(padding).consumeWindowInsets(padding))
    }
}

/** A navigation rail beside the content, and a detail pane too when the layout has one. */
@Composable
private fun RailApp(state: WanderState, layout: AppLayout) {
    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavigationRail(
            header = { AppMark(Modifier.padding(vertical = 8.dp)) },
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
        ) {
            Spacer(Modifier.weight(1f))
            for (tab in Tab.entries) {
                val selected = state.tab == tab
                NavigationRailItem(
                    selected = selected,
                    onClick = { state.showTab(tab) },
                    icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                    label = { Text(tab.label) },
                )
            }
            Spacer(Modifier.weight(1f))
        }
        val contentModifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End))
        val opened = state.opened
        when {
            layout.hasDetailPane -> TabWithDetail(state, layout, contentModifier)
            opened != null -> DestinationDetail(opened, state, layout, onBack = { state.opened = null }, contentModifier)
            else -> TabContent(state, layout, contentModifier)
        }
    }
}

/** A permanent navigation drawer, the grid and a wide detail pane. */
@Composable
private fun DrawerApp(state: WanderState) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                Modifier.width(264.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
            ) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppMark()
                    Text(
                        "Wander",
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                for (tab in Tab.entries) {
                    val selected = state.tab == tab
                    NavigationDrawerItem(
                        label = { Text(tab.label) },
                        selected = selected,
                        onClick = { state.showTab(tab) },
                        icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                        badge = if (tab == Tab.Saved) {
                            { Text("${state.saved.size}") }
                        } else {
                            null
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        },
    ) {
        TabWithDetail(
            state, AppLayout.LargeDesktop,
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)),
        )
    }
}
