package com.apadmi.cmpdevicepreview.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Commute
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apadmi.cmpdevicepreview.sample.resources.Res
import com.apadmi.cmpdevicepreview.sample.resources.cancel
import com.apadmi.cmpdevicepreview.sample.resources.empty_saved_message
import com.apadmi.cmpdevicepreview.sample.resources.empty_saved_title
import com.apadmi.cmpdevicepreview.sample.resources.profile_since
import com.apadmi.cmpdevicepreview.sample.resources.saved_places
import com.apadmi.cmpdevicepreview.sample.resources.setting_currency
import com.apadmi.cmpdevicepreview.sample.resources.setting_reminders
import com.apadmi.cmpdevicepreview.sample.resources.setting_transport
import com.apadmi.cmpdevicepreview.sample.resources.sign_out
import com.apadmi.cmpdevicepreview.sample.resources.sign_out_message
import com.apadmi.cmpdevicepreview.sample.resources.sign_out_title
import com.apadmi.cmpdevicepreview.sample.resources.stat_countries
import com.apadmi.cmpdevicepreview.sample.resources.stat_trips
import com.apadmi.cmpdevicepreview.sample.resources.tab_saved
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch

private val currencies = listOf("GBP", "EUR", "USD", "JPY")

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
            Text(
                stringResource(Res.string.tab_saved),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                pluralStringResource(Res.plurals.saved_places, saved.size, saved.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val empty: @Composable () -> Unit = {
        EmptyState(
            Icons.Outlined.FavoriteBorder,
            stringResource(Res.string.empty_saved_title),
            stringResource(Res.string.empty_saved_message),
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
 * A profile and a few settings, in a column that stops growing on wide screens. The currency is
 * picked from a menu, the preferred transport from a bottom sheet, and signing out asks first in a
 * dialog.
 *
 * @param state The app state that supplies the number of saved destinations in the profile stats.
 * @param modifier The modifier for the screen.
 */
@Composable
fun ProfileScreen(state: WanderState, modifier: Modifier = Modifier) {
    var reminders by remember { mutableStateOf(true) }
    var currency by remember { mutableStateOf(currencies.first()) }
    var isPickingCurrency by remember { mutableStateOf(false) }
    var transport by remember { mutableStateOf(Transport.entries.first()) }
    var isPickingTransport by remember { mutableStateOf(false) }
    var isSigningOut by remember { mutableStateOf(false) }
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
                    Text(
                        "Alex Rivera",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(stringResource(Res.string.profile_since), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("14", stringResource(Res.string.stat_trips), Modifier.weight(1f))
                Stat("9", stringResource(Res.string.stat_countries), Modifier.weight(1f))
                Stat("${state.saved.size}", stringResource(Res.string.tab_saved), Modifier.weight(1f))
            }
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column {
                    SettingRow(Icons.Rounded.NotificationsActive, stringResource(Res.string.setting_reminders)) {
                        Switch(checked = reminders, onCheckedChange = { reminders = it })
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Rounded.Payments, stringResource(Res.string.setting_currency), onClick = { isPickingCurrency = true }) {
                        // The menu opens from the value, at the screen's edge.
                        Box {
                            SettingValue(currency)
                            DropdownMenu(expanded = isPickingCurrency, onDismissRequest = { isPickingCurrency = false }) {
                                for (option in currencies) {
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            currency = option
                                            isPickingCurrency = false
                                        },
                                        trailingIcon = if (option == currency) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                                        } else {
                                            null
                                        },
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        Icons.Rounded.Commute,
                        stringResource(Res.string.setting_transport),
                        onClick = { isPickingTransport = true },
                    ) {
                        SettingValue(stringResource(transport.label))
                    }
                }
            }
            OutlinedButton(onClick = { isSigningOut = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.sign_out))
            }
        }
    }
    if (isPickingTransport) {
        TransportSheet(transport, onPick = { transport = it }, onDismiss = { isPickingTransport = false })
    }
    if (isSigningOut) {
        AlertDialog(
            onDismissRequest = { isSigningOut = false },
            icon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null) },
            title = { Text(stringResource(Res.string.sign_out_title)) },
            text = { Text(stringResource(Res.string.sign_out_message)) },
            confirmButton = { TextButton(onClick = { isSigningOut = false }) { Text(stringResource(Res.string.sign_out)) } },
            dismissButton = { TextButton(onClick = { isSigningOut = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

/** The ways of travelling in a bottom sheet, which slides away once one is picked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransportSheet(
    transport: Transport,
    onPick: (Transport) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            stringResource(Res.string.setting_transport),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        for (option in Transport.entries) {
            ListItem(
                headlineContent = { Text(stringResource(option.label)) },
                trailingContent = if (option == transport) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                } else {
                    null
                },
                modifier = Modifier.clickable {
                    onPick(option)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
            )
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
            Text(
                value,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            title,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
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
