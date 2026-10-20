package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

internal const val SettingsStorageKey = "cmpdevicepreview.settings"

// Replaces the platform's store, for tests.
internal val LocalSettingsStore = compositionLocalOf<SettingsStore?> { null }

internal interface SettingsStore {
    // The settings in the page's URL: always null off the web, and on it when the URL has none.
    fun readLink(): String?

    fun readSaved(): String?

    // Keeps the settings in the URL and in storage, where the platform has them; an empty string clears them.
    fun write(encoded: String)
}

// A store that can't be read or written is as good as none: it must never take the host down.
internal class KeyValueSettingsStore(
    private val load: () -> String?,
    private val save: (String?) -> Unit,
) : SettingsStore {
    override fun readLink(): String? = null

    override fun readSaved(): String? = runCatching(load).getOrNull()?.ifEmpty { null }

    override fun write(encoded: String) {
        runCatching { save(encoded.ifEmpty { null }) }
    }
}

@Composable
internal expect fun rememberSettingsStore(): SettingsStore?
