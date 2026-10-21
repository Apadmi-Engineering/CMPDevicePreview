package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import com.apadmi.cmpdevicepreview.core.Appearance
import com.apadmi.cmpdevicepreview.core.Backdrop
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.DeviceOrientation
import com.apadmi.cmpdevicepreview.core.DeviceType
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.core.displayScaleSteps
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SettingsLinkTest {
    private val initial = PreviewState(SimulatedDevice(DevicePresets.galaxyS25), PreviewSettings())
    private val iPad = assertNotNull(DevicePresets.byId("apple-ipad-pro-13-m4"))

    private object CustomDevice : Device {
        override val name: String = "Mine"
        override val type: DeviceType = DeviceType.Other
        override val portraitSize: Size = Size(400f, 800f)
        override val devicePixelRatio: Float = 2f
    }

    private fun PreviewState.roundTripped(from: PreviewState = initial) = decodeSettings(encodeSettings(this, from), from)

    private fun decoded(encoded: String, from: PreviewState = initial) = decodeSettings(encoded, from)

    private fun configurationOf(encoded: String) = decoded(encoded).simulatedDevice.configuration

    @Test
    fun theInitialStateIsWrittenAsNothing() {
        assertEquals("", encodeSettings(initial, initial))
        assertEquals(initial, decoded(""))
    }

    @Test
    fun aPresetInLandscapeAndDarkIsWrittenAsALink() {
        val state = PreviewState(
            SimulatedDevice(iPad, DeviceConfiguration(orientation = DeviceOrientation.Landscape, appearance = Appearance.Dark)),
            PreviewSettings(),
        )
        assertEquals("dp.device=apple-ipad-pro-13-m4&dp.orientation=landscape&dp.appearance=dark", encodeSettings(state, initial))
        assertEquals(state, state.roundTripped())
    }

    @Test
    fun everySettingSurvivesBeingWrittenAndRead() {
        val pixel = DevicePresets.pixel9
        val state = PreviewState(
            SimulatedDevice(
                pixel,
                DeviceConfiguration(
                    orientation = DeviceOrientation.Landscape,
                    appearance = Appearance.Dark,
                    isFrameVisible = false,
                    isSystemUiVisible = false,
                    isKeyboardVisible = true,
                    fontScale = 1.3f,
                    displayScale = pixel.displayScaleSteps.last().scale,
                    layoutDirection = LayoutDirection.Rtl,
                    language = "pt-BR",
                ),
            ),
            PreviewSettings(backdrop = Backdrop.Auto, isDotGridVisible = false, isPreviewModeEnabled = false),
        )
        assertEquals(state, state.roundTripped())
    }

    @Test
    fun aLanguageIsWrittenAsItsTag() {
        val state = initial.copy(simulatedDevice = SimulatedDevice(DevicePresets.galaxyS25, DeviceConfiguration(language = "cy")))
        assertEquals("dp.language=cy", encodeSettings(state, initial))
        assertEquals(state, state.roundTripped())
    }

    @Test
    fun aLanguageIsKeptAsWrittenWhateverItsCase() {
        assertEquals("PT-br", configurationOf("dp.language=PT-br").language)
    }

    @Test
    fun aValueThatIsNotALanguageTagLeavesTheInitialLanguage() {
        for (value in listOf("", "english", "e", "en_GB", "en-", "en-GB-x-y-z-extra", "en%20GB", "%%", "a".repeat(200))) {
            assertNull(configurationOf("dp.language=$value").language, value)
        }
    }

    @Test
    fun customSettingsAreWrittenUnderTheirOwnKeys() {
        val state = initial.copy(settingValues = PanelSettingValues().with(MockApi, true).with(Environment, "Staging"))
        assertEquals("dp.x.mockApi=1&dp.x.env=Staging", encodeSettings(state, initial))
        assertEquals(state, state.roundTripped())
    }

    @Test
    fun aChoiceIsEscapedInTheLinkAndReadBackAsWritten() {
        val state = initial.copy(settingValues = PanelSettingValues().with(Environment, "São Paulo"))
        assertEquals("dp.x.env=S%C3%A3o%20Paulo", encodeSettings(state, initial))
        assertEquals(state, state.roundTripped())
    }

    @Test
    fun aChoiceWithALinksOwnCharactersIsEscaped() {
        val symbols = PanelSetting.choice(key = "symbols", title = "Symbols", options = listOf("None", "a&b=c+d #1%/"))
        val state = initial.copy(settingValues = PanelSettingValues().with(symbols, "a&b=c+d #1%/"))
        assertEquals("dp.x.symbols=a%26b%3Dc%2Bd%20%231%25%2F", encodeSettings(state, initial))
        assertEquals(state, state.roundTripped())
    }

    @Test
    fun aFreeformWindowIsWrittenWithItsSize() {
        val state = PreviewState(SimulatedDevice(FreeformDevice(Size(834f, 1112f))), PreviewSettings())
        assertEquals("dp.device=freeform&dp.size=834x1112", encodeSettings(state, initial))
        assertEquals(state, state.roundTripped())
    }

    @Test
    fun whatTheHostStartsWithIsWhatIsLeftOut() {
        val darkRtl = PreviewState(
            SimulatedDevice(
                DevicePresets.pixel9,
                DeviceConfiguration(appearance = Appearance.Dark, fontScale = 1.3f, layoutDirection = LayoutDirection.Rtl, language = "cy"),
            ),
            PreviewSettings(isDotGridVisible = false),
        )
        assertEquals("", encodeSettings(darkRtl, darkRtl))

        val lightFollowingTheHost = darkRtl.copy(
            simulatedDevice = darkRtl.simulatedDevice.copy(
                configuration = darkRtl.simulatedDevice.configuration.copy(
                    appearance = Appearance.Light,
                    layoutDirection = null,
                    language = null,
                ),
            ),
        )
        assertEquals("dp.appearance=light&dp.direction=auto&dp.language=auto", encodeSettings(lightFollowingTheHost, darkRtl))
        assertEquals(lightFollowingTheHost, lightFollowingTheHost.roundTripped(darkRtl))
    }

    @Test
    fun aDeviceThatIsNeitherAPresetNorFreeformIsLeftOut() {
        val state = PreviewState(SimulatedDevice(CustomDevice, DeviceConfiguration(appearance = Appearance.Dark)), PreviewSettings())
        assertEquals("dp.appearance=dark", encodeSettings(state, initial))
    }

    @Test
    fun keysThatAreUnknownAndParamsOfTheHostAreIgnored() {
        assertEquals(
            DeviceConfiguration(appearance = Appearance.Dark),
            configurationOf("dp.nope=1&theme=light&appearance=light&dp.appearance=dark&dp.&dp.orientation"),
        )
    }

    @Test
    fun theLastOfARepeatedKeyWins() {
        assertEquals(Appearance.Dark, configurationOf("dp.appearance=light&dp.appearance=dark").appearance)
    }

    @Test
    fun aValueThatIsNotValidLeavesThatSettingAsItWas() {
        val state = decoded("dp.appearance=purple&dp.orientation=landscape&dp.keyboard=maybe&dp.bezel=&dp.direction=up&dp.backdrop=neon&dp.dotGrid=2")
        assertEquals(DeviceConfiguration(orientation = DeviceOrientation.Landscape), state.simulatedDevice.configuration)
        assertEquals(PreviewSettings(), state.previewSettings)
    }

    @Test
    fun anUnknownDeviceLeavesTheInitialOne() {
        assertEquals(DevicePresets.galaxyS25, decoded("dp.device=apple-iphone-99").simulatedDevice.device)
    }

    @Test
    fun aFreeformWindowNeedsAValidSize() {
        for (link in listOf(
            "dp.device=freeform",
            "dp.device=freeform&dp.size=abc",
            "dp.device=freeform&dp.size=300",
            "dp.device=freeform&dp.size=300x",
            "dp.device=freeform&dp.size=1x2x3",
            "dp.device=freeform&dp.size=1.5x2",
        )) {
            assertEquals(DevicePresets.galaxyS25, decoded(link).simulatedDevice.device, link)
        }
    }

    @Test
    fun aFreeformSizeIsKeptWithinTheLimits() {
        assertEquals(
            FreeformDevice(Size(MinFreeformSize, MaxFreeformSize)),
            decoded("dp.device=freeform&dp.size=10x99999").simulatedDevice.device,
        )
    }

    @Test
    fun scalesThatAreNotNumbersAreIgnored() {
        for (value in listOf("abc", "NaN", "Infinity", "-1", "0", "")) {
            val configuration = configurationOf("dp.fontScale=$value&dp.displayScale=$value")
            assertEquals(DeviceConfiguration(), configuration, value)
        }
    }

    @Test
    fun scalesThatAreNotStepsAreSnappedToTheNearestOne() {
        val configuration = configurationOf("dp.fontScale=1.31&dp.displayScale=1.01")
        assertEquals(1.3f, configuration.fontScale)
        assertEquals(1f, configuration.displayScale)
    }

    @Test
    fun aKeyboardTheDeviceLacksIsDropped() {
        assertEquals(false, configurationOf("dp.device=freeform&dp.size=500x500&dp.keyboard=1").isKeyboardVisible)
        assertEquals(true, configurationOf("dp.keyboard=1").isKeyboardVisible)
    }

    @Test
    fun valuesAreReadWithoutRegardToCaseOrEscapes() {
        val plain = decoded("dp.device=apple-ipad-pro-13-m4&dp.orientation=landscape&dp.appearance=dark&dp.backdrop=auto")
        assertEquals(
            plain,
            decoded("dp.device=apple%2Dipad-pro-13-m4&dp%2Eorientation=LANDSCAPE&dp.appearance=Dark&dp.backdrop=AUTO"),
        )
        assertEquals(iPad, plain.simulatedDevice.device)
    }

    @Test
    fun aLeadingQuestionMarkIsAccepted() {
        assertEquals(Appearance.Dark, configurationOf("?dp.appearance=dark").appearance)
    }

    @Test
    fun flagsAreReadAsNumbersOrWords() {
        for (value in listOf("1", "true", "on", "TRUE")) {
            assertEquals(true, configurationOf("dp.keyboard=$value").isKeyboardVisible, value)
        }
        for (value in listOf("0", "false", "off", "Off")) {
            assertEquals(false, configurationOf("dp.bezel=$value").isFrameVisible, value)
        }
    }

    @Test
    fun garbageIsReadAsTheInitialState() {
        assertEquals(initial, decoded("%%%&&&=&dp.&dp.=&===&dp.device"))
    }

    @Test
    fun theSettingsInAQueryStringAreFoundAsWritten() {
        assertEquals("dp.device=x&dp.size=1x1", settingsParams("?tab=2&dp.device=x&dp.size=1x1"))
        assertEquals("dp%2Edevice=x", settingsParams("a=1&dp%2Edevice=x"))
        assertNull(settingsParams("?tab=2"))
        assertNull(settingsParams(""))
    }

    @Test
    fun swappingTheSettingsKeepsEveryOtherPairAsWritten() {
        assertEquals("?tab=2&q=a%20b&dp.new=2", withSettingsParams("?tab=2&dp.old=1&q=a%20b", "dp.new=2"))
        assertEquals("?dp.new=2", withSettingsParams("", "dp.new=2"))
    }

    @Test
    fun swappingInNothingLeavesNoStrayCharacters() {
        assertEquals("", withSettingsParams("?dp.device=x", ""))
        assertEquals("?a=1", withSettingsParams("?a=1&dp.device=x", ""))
        assertEquals("", withSettingsParams("", ""))
    }
}
