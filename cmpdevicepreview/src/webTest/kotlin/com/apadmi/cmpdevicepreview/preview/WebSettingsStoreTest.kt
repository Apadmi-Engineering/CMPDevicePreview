@file:OptIn(ExperimentalWasmJsInterop::class)

package com.apadmi.cmpdevicepreview.preview

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.browser.window

class WebSettingsStoreTest {
    private val store = WebSettingsStore()
    private lateinit var originalUrl: String

    @BeforeTest
    fun startClean() {
        originalUrl = window.location.pathname + window.location.search + window.location.hash
        window.localStorage.removeItem(SettingsStorageKey)
    }

    @AfterTest
    fun restore() {
        window.localStorage.removeItem(SettingsStorageKey)
        window.history.replaceState(null, "", originalUrl)
    }

    private fun openPage(search: String, hash: String = "") {
        window.history.replaceState(null, "", window.location.pathname + search + hash)
    }

    @Test
    fun whatIsWrittenIsReadBackFromStorage() {
        store.write("dp.orientation=landscape&dp.appearance=dark")

        assertEquals("dp.orientation=landscape&dp.appearance=dark", store.readSaved())
        assertEquals("dp.orientation=landscape&dp.appearance=dark", window.localStorage.getItem(SettingsStorageKey))
    }

    @Test
    fun theLinkCarriesWhatIsWrittenAndKeepsThePagesOwnParamsAndHash() {
        openPage("?tab=2&q=a%20b", "#section")

        store.write("dp.device=apple-iphone-17")

        assertEquals("?tab=2&q=a%20b&dp.device=apple-iphone-17", window.location.search)
        assertEquals("#section", window.location.hash)
        assertEquals("dp.device=apple-iphone-17", store.readLink())
    }

    @Test
    fun aLinkIsReadAsItWasOpened() {
        openPage("?dp.device=freeform&dp.size=834x1112&tab=2")

        assertEquals("dp.device=freeform&dp.size=834x1112", store.readLink())
    }

    @Test
    fun theLinkHasNoSettingsWhenThePageHasNone() {
        openPage("?tab=2")

        assertNull(store.readLink())
        assertNull(store.readSaved())
    }

    @Test
    fun writingNothingClearsTheLinkAndStorageAndLeavesThePagesOwnParams() {
        openPage("?tab=2&dp.device=apple-iphone-17", "#section")
        window.localStorage.setItem(SettingsStorageKey, "dp.device=apple-iphone-17")

        store.write("")

        assertEquals("?tab=2", window.location.search)
        assertEquals("#section", window.location.hash)
        assertNull(store.readLink())
        assertNull(store.readSaved())
    }

    @Test
    fun theWholeQueryStringGoesWhenNothingIsLeftInIt() {
        openPage("?dp.device=apple-iphone-17")

        store.write("")

        assertEquals("", window.location.search)
    }
}
