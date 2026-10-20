package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberSettingsStore(): SettingsStore? = remember {
    val defaults = NSUserDefaults.standardUserDefaults
    KeyValueSettingsStore(
        load = { defaults.stringForKey(SettingsStorageKey) },
        save = { value ->
            if (value == null) {
                defaults.removeObjectForKey(SettingsStorageKey)
            } else {
                defaults.setObject(value, forKey = SettingsStorageKey)
            }
        },
    )
}
