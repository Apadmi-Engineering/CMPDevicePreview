package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.prefs.Preferences

private const val PreferencesNode = "com/apadmi/cmpdevicepreview"

@Composable
internal actual fun rememberSettingsStore(): SettingsStore? = remember {
    // Opened on first use, inside the store's guard, as a machine without preferences throws here.
    val preferences by lazy { Preferences.userRoot().node(PreferencesNode) }
    KeyValueSettingsStore(
        load = { preferences.get(SettingsStorageKey, null) },
        save = { value ->
            if (value == null) preferences.remove(SettingsStorageKey) else preferences.put(SettingsStorageKey, value)
            preferences.flush()
        },
    )
}
