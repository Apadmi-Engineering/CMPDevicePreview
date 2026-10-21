package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.apadmi.cmpdevicepreview.core.Appearance
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.PreviewLanguage
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals

private val Languages = listOf(
    PreviewLanguage("en", "English"),
    PreviewLanguage("cy", "Cymraeg"),
    PreviewLanguage("pt", "Português"),
)

@OptIn(ExperimentalTestApi::class)
class SettingsPersistenceTest {
    private fun ComposeUiTest.showView(
        store: FakeSettingsStore,
        isPersistenceEnabled: Boolean = true,
        initialConfiguration: DeviceConfiguration = DeviceConfiguration(),
        languages: List<PreviewLanguage> = emptyList(),
        panel: PanelScope.() -> Unit = DefaultPanel,
    ) {
        setContent {
            CompositionLocalProvider(LocalSettingsStore provides store) {
                Box(Modifier.width(1000.dp).height(800.dp)) {
                    DeviceWithConfigurationView(
                        initialDevice = DevicePresets.pixel9,
                        initialConfiguration = initialConfiguration,
                        panel = panel,
                        isPersistenceEnabled = isPersistenceEnabled,
                        languages = languages,
                    ) {}
                }
            }
        }
    }

    private fun ComposeUiTest.settle() {
        waitForIdle()
        mainClock.advanceTimeBy(1000)
        waitForIdle()
    }

    private fun SemanticsNodeInteractionsProvider.assertSelected(text: String) =
        onNode(hasText(text) and isSelectable()).assertIsSelected()

    @Test
    fun theViewStartsFromTheKeptSettings() = runComposeUiTest {
        showView(FakeSettingsStore(saved = "dp.device=apple-iphone-16-pro&dp.orientation=landscape"))

        onNodeWithText("iPhone 16 Pro").assertExists()
        assertSelected("Landscape")
    }

    @Test
    fun aLinkBeatsTheKeptSettingsAndWhatItOmitsStaysInitial() = runComposeUiTest {
        val store = FakeSettingsStore(link = "dp.orientation=landscape", saved = "dp.appearance=dark&dp.device=apple-iphone-16-pro")
        showView(store)

        assertSelected("Landscape")
        onNodeWithText("Pixel 9").assertExists()
        onAllNodes(hasText("Light") and isSelectable())[0].assertIsSelected()
    }

    @Test
    fun aChangeIsKeptOnceTheViewHasBeenStillForAMoment() = runComposeUiTest {
        val store = FakeSettingsStore()
        showView(store)

        onNode(hasText("Landscape") and isSelectable()).performClick()
        settle()

        assertEquals("dp.orientation=landscape", store.writes.last())
    }

    @Test
    fun theSettingsAreKeptWhilePreviewModeIsOffToo() = runComposeUiTest {
        val store = FakeSettingsStore()
        showView(store)

        onNode(hasText("Landscape") and isSelectable()).performClick()
        onNodeWithText("Preview mode").performClick()
        settle()

        assertEquals("dp.orientation=landscape&dp.preview=0", store.writes.last())
    }

    @Test
    fun resetAllClearsWhatWasKeptAndReturnsToTheInitialDevice() = runComposeUiTest {
        val store = FakeSettingsStore(saved = "dp.device=apple-iphone-16-pro&dp.orientation=landscape&dp.appearance=dark")
        showView(store)
        onNodeWithText("iPhone 16 Pro").assertExists()

        onNodeWithContentDescription("Reset all settings").performClick()
        settle()

        onNodeWithText("Pixel 9").assertExists()
        assertSelected("Portrait")
        assertEquals("", store.writes.last())
    }

    @Test
    fun whatTheHostStartsWithIsNotKept() = runComposeUiTest {
        val store = FakeSettingsStore()
        showView(store, initialConfiguration = DeviceConfiguration(appearance = Appearance.Dark))

        settle()

        assertEquals(listOf(""), store.writes.distinct())
    }

    @Test
    fun nothingIsReadOrKeptWhenPersistenceIsOff() = runComposeUiTest {
        val store = FakeSettingsStore(saved = "dp.orientation=landscape", link = "dp.appearance=dark")
        showView(store, isPersistenceEnabled = false)

        assertSelected("Portrait")
        onNode(hasText("Landscape") and isSelectable()).performClick()
        settle()

        assertEquals(0, store.reads)
        assertEquals(emptyList(), store.writes)
    }

    @Test
    fun aKeptLanguageTheHostOffersIsShownWithoutRegardToCase() = runComposeUiTest {
        showView(FakeSettingsStore(saved = "dp.language=CY"), languages = Languages)

        onNodeWithText("Cymraeg").assertExists()
    }

    @Test
    fun aKeptLanguageTheHostDoesNotOfferFallsBackToTheFirstAndIsNotKept() = runComposeUiTest {
        val store = FakeSettingsStore(saved = "dp.language=fr")
        showView(store, languages = Languages)
        settle()

        onNodeWithText("English").assertExists()
        assertEquals("", store.writes.last())
    }

    @Test
    fun aKeptLanguageIsIgnoredWhenTheHostOffersNone() = runComposeUiTest {
        val store = FakeSettingsStore(saved = "dp.language=cy")
        showView(store)
        settle()

        onNodeWithText("LANGUAGE").assertDoesNotExist()
        assertEquals("", store.writes.last())
    }

    @Test
    fun aChosenLanguageIsKeptAndTheFirstIsWhatTheHostStartsWith() = runComposeUiTest {
        val store = FakeSettingsStore()
        showView(store, languages = Languages)
        settle()
        assertEquals("", store.writes.last())

        onNodeWithText("English").performScrollTo().performClick()
        onNodeWithText("Cymraeg").performClick()
        settle()

        assertEquals("dp.language=cy", store.writes.last())
    }

    @Test
    fun aLinkCannotChangeASettingWhoseControlIsHidden() = runComposeUiTest {
        val store = FakeSettingsStore(link = "dp.backdrop=dark&dp.appearance=dark")
        showView(store, panel = { defaults { backdrop.isVisible = false } })
        settle()

        // The appearance is in the panel, so the link sets it; the backdrop isn't, so it stays as the host started it.
        assertSelected("Dark")
        assertEquals("dp.appearance=dark", store.writes.last())
    }

    @Test
    fun aLinkSetsYourOwnSettingAndResetAllClearsIt() = runComposeUiTest {
        val store = FakeSettingsStore(link = "dp.x.mockApi=1&dp.x.unknown=1")
        showView(
            store,
            panel = {
                defaults()
                setting(MockApi)
            },
        )
        settle()
        onNodeWithText("Mock API").assertIsOn()
        assertEquals("dp.x.mockApi=1", store.writes.last())

        onNodeWithContentDescription("Reset all settings").performClick()
        settle()
        onNodeWithText("Mock API").assertIsOff()
        assertEquals("", store.writes.last())
    }

    private class FakeSettingsStore(var link: String? = null, var saved: String? = null) : SettingsStore {
        val writes = mutableListOf<String>()
        var reads = 0

        override fun readLink(): String? = link.also { reads++ }

        override fun readSaved(): String? = saved.also { reads++ }

        override fun write(encoded: String) {
            writes += encoded
        }
    }
}
