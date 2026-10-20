package com.apadmi.cmpdevicepreview.preview

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val PreferencesName = "cmpdevicepreview"

@Composable
internal actual fun rememberSettingsStore(): SettingsStore? {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        KeyValueSettingsStore(
            load = { preferences.getString(SettingsStorageKey, null) },
            save = { value ->
                val editor = preferences.edit()
                if (value == null) editor.remove(SettingsStorageKey) else editor.putString(SettingsStorageKey, value)
                editor.apply()
            },
        )
    }
}
