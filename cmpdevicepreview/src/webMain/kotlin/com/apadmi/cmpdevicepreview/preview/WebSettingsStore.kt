@file:OptIn(ExperimentalWasmJsInterop::class)

package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

// Blocked storage, or Safari's limit on how often the URL may change, throw: neither may take the host down.
internal class WebSettingsStore : SettingsStore {
    override fun readLink(): String? = runCatching { settingsParams(window.location.search) }.getOrNull()

    override fun readSaved(): String? = runCatching { window.localStorage.getItem(SettingsStorageKey) }.getOrNull()

    override fun write(encoded: String) {
        runCatching {
            if (encoded.isEmpty()) {
                window.localStorage.removeItem(SettingsStorageKey)
            } else {
                window.localStorage.setItem(SettingsStorageKey, encoded)
            }
        }
        runCatching {
            val location = window.location
            window.history.replaceState(null, "", location.pathname + withSettingsParams(location.search, encoded) + location.hash)
        }
    }
}

@Composable
internal actual fun rememberSettingsStore(): SettingsStore? = remember { WebSettingsStore() }
