package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.apadmi.cmpdevicepreview.core.Appearance
import com.apadmi.cmpdevicepreview.core.Backdrop
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.DeviceOrientation
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewLanguage
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val AttributionText = "Built with CMPDevicePreview"

private val Languages = listOf(
    PreviewLanguage("en", "English"),
    PreviewLanguage("cy", "Cymraeg"),
    PreviewLanguage("pt", "Português"),
)

@OptIn(ExperimentalTestApi::class)
class ConfigurationViewTest {
    @Test
    fun yourOwnItemsAreShownInOrderAsWideAsTheSettings() {
        val shown = mutableListOf<String>()
        var width = -1
        var primary = Color.Unspecified
        renderComposeScene(300, 4000) {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                panel = {
                    defaults()
                    item {
                        shown += "first"
                        primary = MaterialTheme.colorScheme.primary
                        Box(Modifier.fillMaxWidth().onSizeChanged { width = it.width })
                    }
                    item { shown += "second" }
                },
            )
        }
        assertEquals(listOf("first", "second"), shown)
        // The panel's 16 dp margins, as its settings have.
        assertEquals(300 - 2 * 16, width)
        assertEquals(PanelColors.Accent, primary, "inside the panel's theme")
    }

    @Test
    fun yourOwnTitleAndIconReplaceTheDefaultHeader() = runComposeUiTest {
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                panel = {
                    defaults {
                        header.title = "Acme Preview"
                        header.icon = { Box(Modifier.fillMaxSize().testTag("logo")) }
                    }
                },
            )
        }
        onNodeWithText("Acme Preview").assertExists()
        onNodeWithText("Device Preview").assertDoesNotExist()
        // The icon fills the header's square, and no more.
        onNodeWithTag("logo").assertWidthIsEqualTo(28.dp).assertHeightIsEqualTo(28.dp)
    }

    @Test
    fun theLinkToTheProjectIsShownUnlessItIsHidden() = runComposeUiTest {
        var isAttributionVisible by mutableStateOf(true)
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                panel = { defaults { attribution.isVisible = isAttributionVisible } },
            )
        }
        onNodeWithText(AttributionText).assertExists()

        isAttributionVisible = false
        waitForIdle()
        onNodeWithText(AttributionText).assertDoesNotExist()
    }

    @Test
    fun theLinkOpensTheProjectsRepository() = runComposeUiTest {
        val opened = mutableListOf<String>()
        setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler { opened += it }) {
                ConfigurationView(
                    simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                    onSimulatedDeviceChange = {},
                    previewSettings = PreviewSettings(),
                    onPreviewSettingsChange = {},
                    modifier = Modifier.width(300.dp).fillMaxSize(),
                )
            }
        }
        onNodeWithText(AttributionText).performScrollTo().performClick()
        assertEquals(listOf("https://github.com/Apadmi-Engineering/CMPDevicePreview"), opened)
    }

    @Test
    fun aLinkThatCannotBeOpenedDoesNotCrashTheApp() = runComposeUiTest {
        var attempts = 0
        setContent {
            CompositionLocalProvider(
                LocalUriHandler provides uriHandler {
                    attempts++
                    throw IllegalArgumentException("No app can open $it")
                },
            ) {
                ConfigurationView(
                    simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                    onSimulatedDeviceChange = {},
                    previewSettings = PreviewSettings(),
                    onPreviewSettingsChange = {},
                    modifier = Modifier.width(300.dp).fillMaxSize(),
                )
            }
        }
        onNodeWithText(AttributionText).performScrollTo().performClick()
        assertEquals(1, attempts)
    }

    @Test
    fun aTypedSizeIsFittedToTheDevice() = runComposeUiTest {
        var changed: SimulatedDevice? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(FreeformDevice(Size(800f, 800f)), DeviceConfiguration(displayScale = 1.5f)),
                onSimulatedDeviceChange = { changed = it },
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        val width = onAllNodes(hasSetTextAction())[0]
        width.performTextReplacement("300")
        width.performImeAction()

        val resized = assertNotNull(changed)
        assertEquals(FreeformDevice(Size(300f, 800f)), resized.device)
        // A window 300 dp across offers no display size larger than the default.
        assertEquals(1f, resized.configuration.displayScale)
    }

    @Test
    fun theBackdropChangesThePreviewSettingsNotTheDevice() = runComposeUiTest {
        var device: SimulatedDevice? = null
        var settings: PreviewSettings? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = { device = it },
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = { settings = it },
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        onNode(hasText("Auto") and isSelectable()).performScrollTo().performClick()
        assertEquals(PreviewSettings(backdrop = Backdrop.Auto), settings)

        onNodeWithText("Dot grid").performScrollTo().performClick()
        assertEquals(PreviewSettings(isDotGridVisible = false), settings)
        assertNull(device)
    }

    @Test
    fun theResetButtonsReturnTheTextAndDisplaySizesToTheirDefaults() = runComposeUiTest {
        var changed: SimulatedDevice? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(
                    DevicePresets.pixel9,
                    DeviceConfiguration(fontScale = 1.5f, displayScale = 0.85f),
                ),
                onSimulatedDeviceChange = { changed = it },
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        onNodeWithContentDescription("Reset Text size").performScrollTo().assertIsEnabled().performClick()
        assertEquals(DeviceConfiguration(fontScale = 1f, displayScale = 0.85f), assertNotNull(changed).configuration)

        onNodeWithContentDescription("Reset Display size").performScrollTo().assertIsEnabled().performClick()
        assertEquals(DeviceConfiguration(fontScale = 1.5f, displayScale = 1f), assertNotNull(changed).configuration)
    }

    @Test
    fun theResetButtonsAreOutOfTheWayWhileTheSizesAreTheDefaults() = runComposeUiTest {
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                initialSimulatedDevice = SimulatedDevice(DevicePresets.pixel9),
            )
        }
        onNodeWithContentDescription("Reset Text size").assertIsNotEnabled()
        onNodeWithContentDescription("Reset Display size").assertIsNotEnabled()
        onNodeWithContentDescription("Reset Layout direction").assertIsNotEnabled()
        onNodeWithContentDescription("Reset all settings").assertIsNotEnabled()
    }

    @Test
    fun resetAllReturnsTheDeviceAndEverySettingToTheInitialOnes() = runComposeUiTest {
        var changedDevice: SimulatedDevice? = null
        var changedSettings: PreviewSettings? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(
                    DevicePresets.iPhone16Pro,
                    DeviceConfiguration(
                        orientation = DeviceOrientation.Landscape,
                        appearance = Appearance.Dark,
                        isFrameVisible = false,
                        fontScale = 1.5f,
                        displayScale = 0.85f,
                        layoutDirection = LayoutDirection.Rtl,
                        language = "cy",
                    ),
                ),
                onSimulatedDeviceChange = { changedDevice = it },
                previewSettings = PreviewSettings(backdrop = Backdrop.Dark, isDotGridVisible = false),
                onPreviewSettingsChange = { changedSettings = it },
                modifier = Modifier.width(300.dp).fillMaxSize(),
                initialSimulatedDevice = SimulatedDevice(DevicePresets.pixel9),
            )
        }
        onNodeWithContentDescription("Reset all settings").assertIsEnabled().performClick()
        assertEquals(SimulatedDevice(DevicePresets.pixel9), changedDevice)
        assertEquals(PreviewSettings(), changedSettings)
    }

    @Test
    fun resetAllGoesBackToWhatTheHostStartedWithNotTheLibrarysDefaults() = runComposeUiTest {
        val started = SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(appearance = Appearance.Dark, fontScale = 1.3f))
        val startedSettings = PreviewSettings(backdrop = Backdrop.Auto)
        var changedDevice: SimulatedDevice? = null
        var changedSettings: PreviewSettings? = null
        var current by mutableStateOf(SimulatedDevice(DevicePresets.pixel9))
        setContent {
            ConfigurationView(
                simulatedDevice = current,
                onSimulatedDeviceChange = { changedDevice = it },
                previewSettings = startedSettings,
                onPreviewSettingsChange = { changedSettings = it },
                modifier = Modifier.width(300.dp).fillMaxSize(),
                initialSimulatedDevice = started,
                initialPreviewSettings = startedSettings,
            )
        }
        onNodeWithContentDescription("Reset all settings").assertIsEnabled().performClick()
        assertEquals(started, changedDevice)
        assertEquals(startedSettings, changedSettings)

        current = started
        waitForIdle()
        onNodeWithContentDescription("Reset all settings").assertIsNotEnabled()
    }

    @Test
    fun resetAllIsOfferedWhenOnlyTheDeviceOrOnlyThePreviewSettingsDiffer() = runComposeUiTest {
        var simulatedDevice by mutableStateOf(SimulatedDevice(DevicePresets.pixel9))
        var previewSettings by mutableStateOf(PreviewSettings())
        setContent {
            ConfigurationView(
                simulatedDevice = simulatedDevice,
                onSimulatedDeviceChange = {},
                previewSettings = previewSettings,
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                initialSimulatedDevice = SimulatedDevice(DevicePresets.pixel9),
            )
        }
        onNodeWithContentDescription("Reset all settings").assertIsNotEnabled()

        simulatedDevice = SimulatedDevice(DevicePresets.iPhone16Pro)
        waitForIdle()
        onNodeWithContentDescription("Reset all settings").assertIsEnabled()

        simulatedDevice = SimulatedDevice(DevicePresets.pixel9)
        previewSettings = PreviewSettings(isDotGridVisible = false)
        waitForIdle()
        onNodeWithContentDescription("Reset all settings").assertIsEnabled()
    }

    @Test
    fun theLayoutDirectionResetFollowsTheHostAgain() = runComposeUiTest {
        var changed: SimulatedDevice? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(layoutDirection = LayoutDirection.Rtl)),
                onSimulatedDeviceChange = { changed = it },
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        onNodeWithContentDescription("Reset Layout direction").performScrollTo().assertIsEnabled().performClick()
        assertNull(assertNotNull(changed).configuration.layoutDirection)
    }

    @Test
    fun thereIsNoLanguageSettingWithoutLanguages() = runComposeUiTest {
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(language = "cy")),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        onNodeWithText("LANGUAGE").assertDoesNotExist()
        onNodeWithText("Cymraeg").assertDoesNotExist()
    }

    @Test
    fun theLanguageSettingShowsTheChosenLanguageAndOffersAllWhenOpened() = runComposeUiTest {
        var changed: SimulatedDevice? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(language = "cy")),
                onSimulatedDeviceChange = { changed = it },
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                languages = Languages,
            )
        }
        onNodeWithText("LANGUAGE").assertExists()
        onNodeWithText("English").assertDoesNotExist()
        onNodeWithText("Português").assertDoesNotExist()

        onNodeWithText("Cymraeg").performScrollTo().performClick()
        onAllNodesWithText("Cymraeg").assertCountEquals(2)
        onNodeWithText("English").assertExists()
        assertNull(changed)

        onNodeWithText("Português").performClick()
        assertEquals("pt", assertNotNull(changed).configuration.language)
        onNodeWithText("English").assertDoesNotExist()
    }

    @Test
    fun theFirstLanguageIsShownWhenTheConfigurationNamesNoneOfThem() = runComposeUiTest {
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(language = "fr")),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                languages = Languages,
            )
        }
        onNodeWithText("English").assertExists()
        onNodeWithContentDescription("Reset Language").assertIsNotEnabled()
    }

    @Test
    fun theLanguageResetReturnsToTheFirstLanguage() = runComposeUiTest {
        var changed: SimulatedDevice? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(language = "pt")),
                onSimulatedDeviceChange = { changed = it },
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                languages = Languages,
            )
        }
        onNodeWithContentDescription("Reset Language").performScrollTo().assertIsEnabled().performClick()
        assertEquals("en", assertNotNull(changed).configuration.language)
    }

    @Test
    fun theSlidersAreNamedAndAnnounceTheirValue() = runComposeUiTest {
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(fontScale = 1.5f)),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
            )
        }
        val value = { text: String -> SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text) }
        onNodeWithContentDescription("Text size").performScrollTo().assert(value("150%"))
        onNodeWithContentDescription("Display size").performScrollTo().assert(value("Default"))
    }

    @Test
    fun thePreviewModeSwitchIsLastUnlessPlacedElsewhere() = runComposeUiTest {
        var panel: PanelScope.() -> Unit by mutableStateOf(DefaultPanel)
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                panel = panel,
            )
        }
        // Unclipped, as the end of the panel is below the window.
        val top = { text: String -> onNodeWithText(text).fetchSemanticsNode().positionInRoot.y }
        assertTrue(top("Dot grid") < top("Preview mode"))
        assertTrue(top("Preview mode") < top(AttributionText))

        panel = {
            previewMode()
            defaults()
        }
        waitForIdle()
        assertTrue(top("Preview mode") < top("FRAME"))
    }

    @Test
    fun controlsAreShownInTheOrderTheyAreWrittenAndNothingElse() = runComposeUiTest {
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                panel = {
                    appearance()
                    item { Text("Mine") }
                    orientation()
                },
            )
        }
        val top = { text: String -> onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top }
        assertTrue(top("APPEARANCE") < top("Mine"))
        assertTrue(top("Mine") < top("ORIENTATION"))
        onNodeWithText("FRAME").assertDoesNotExist()
        onNodeWithText("Preview mode").assertDoesNotExist()
        onNodeWithText(AttributionText).assertDoesNotExist()
        onNodeWithContentDescription("Reset all settings").assertDoesNotExist()
    }

    @Test
    fun theTextSizeSliderOnlyOffersSizesInItsRange() = runComposeUiTest {
        var changed: SimulatedDevice? = null
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(fontScale = 3f)),
                onSimulatedDeviceChange = { changed = it },
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                panel = { textSize(range = 0.85f..2f) },
            )
        }
        val slider = onNodeWithContentDescription("Text size")
        slider.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "200%"))
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        assertEquals(0.85f, assertNotNull(changed).configuration.fontScale)
    }

    @Test
    fun yourOwnSettingsChangeTheirValues() = runComposeUiTest {
        var values by mutableStateOf(PanelSettingValues())
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                panel = {
                    section("Debug") {
                        setting(MockApi)
                        setting(Environment)
                    }
                },
                settingValues = values,
                onSettingValuesChange = { values = it },
            )
        }
        onNodeWithText("DEBUG").assertExists()
        onNodeWithText("Mock API").performClick()
        assertTrue(values[MockApi])

        onNodeWithText("Dev").performClick()
        onNodeWithText("São Paulo").performClick()
        assertEquals("São Paulo", values[Environment])
        assertTrue(values[MockApi])
    }

    @Test
    fun aChoiceShowsItsResetOnlyWhileItIsChanged() = runComposeUiTest {
        var values by mutableStateOf(PanelSettingValues())
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                panel = { setting(Environment) },
                settingValues = values,
                onSettingValuesChange = { values = it },
            )
        }
        onNodeWithContentDescription("Reset Environment").assertIsNotEnabled()

        values = values.with(Environment, "Staging")
        waitForIdle()
        onNodeWithContentDescription("Reset Environment").assertIsEnabled().performClick()
        assertEquals(PanelSettingValues(), values)
    }

    @Test
    fun resetAllReturnsYourOwnSettingsToTheirDefaults() = runComposeUiTest {
        var values by mutableStateOf(PanelSettingValues().with(MockApi, true))
        setContent {
            ConfigurationView(
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onSimulatedDeviceChange = {},
                previewSettings = PreviewSettings(),
                onPreviewSettingsChange = {},
                modifier = Modifier.width(300.dp).fillMaxSize(),
                panel = {
                    defaults()
                    setting(MockApi)
                },
                settingValues = values,
                onSettingValuesChange = { values = it },
                initialSimulatedDevice = SimulatedDevice(DevicePresets.pixel9),
            )
        }
        onNodeWithContentDescription("Reset all settings").assertIsEnabled().performClick()
        assertEquals(PanelSettingValues(), values)
        onNodeWithContentDescription("Reset all settings").assertIsNotEnabled()
    }
}

/** A [UriHandler] that passes each link it is asked to open to [onOpen], rather than to a browser. */
private fun uriHandler(onOpen: (String) -> Unit) = object : UriHandler {
    override fun openUri(uri: String) = onOpen(uri)
}
