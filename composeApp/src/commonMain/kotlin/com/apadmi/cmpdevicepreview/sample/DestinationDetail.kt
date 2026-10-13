package com.apadmi.cmpdevicepreview.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A destination in full. On a phone it fills the screen, its illustration running up behind the
 * status bar; turned sideways, the illustration sits beside the text.
 */
@Composable
fun DestinationDetail(
    destination: Destination,
    state: WanderState,
    layout: AppLayout,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (layout == AppLayout.PhoneLandscape) {
        Row(modifier.fillMaxSize()) {
            Box(Modifier.weight(0.45f).fillMaxHeight().padding(12.dp).clip(RoundedCornerShape(24.dp))) {
                SceneryArt(destination, Modifier.matchParentSize())
                ArtButtons(destination, state, onBack)
            }
            DetailBody(
                destination, state,
                Modifier.weight(0.55f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(20.dp),
            )
        }
    } else {
        val edgeToEdge = layout == AppLayout.Phone
        Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (edgeToEdge) 4f / 3.6f else 16f / 10f)
                    .then(if (edgeToEdge) Modifier else Modifier.padding(12.dp).clip(RoundedCornerShape(24.dp)))
            ) {
                SceneryArt(destination, Modifier.matchParentSize())
                ArtButtons(destination, state, onBack, if (edgeToEdge) Modifier.statusBarsPadding() else Modifier)
            }
            DetailBody(
                destination, state,
                Modifier.padding(20.dp).then(if (edgeToEdge) Modifier.navigationBarsPadding() else Modifier),
            )
        }
    }
}

@Composable
private fun ArtButtons(destination: Destination, state: WanderState, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f), contentColor = Color.White),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        }
        Box(Modifier.weight(1f))
        SaveButton(destination, state, onArt = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailBody(destination: Destination, state: WanderState, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text(destination.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Place, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp),
                )
                Text(
                    "${destination.country} · ${destination.category.label}",
                    modifier = Modifier.padding(start = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Fact(Icons.Rounded.Star, "${destination.rating} rating", RatingColor)
            Fact(Icons.Rounded.Bedtime, "${destination.nights} nights")
            Fact(Icons.Rounded.Sell, "from £${destination.price}")
        }
        Text(destination.blurb, style = MaterialTheme.typography.bodyLarge)
        FilledTonalButton(onClick = { state.toggleSaved(destination) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (destination.id in state.saved) "Saved" else "Save")
        }
    }
}

@Composable
private fun Fact(icon: ImageVector, text: String, iconTint: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Text(text, modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** The detail beside the grid, on the layouts with room for both. */
@Composable
fun DetailPane(state: WanderState, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxHeight().padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        DestinationDetail(state.selected, state, AppLayout.Desktop, onBack = null)
    }
}
