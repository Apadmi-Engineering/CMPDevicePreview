package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.renderComposeScene

import com.apadmi.cmpdevicepreview.core.Appearance
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.devices.DevicePresets

import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.rememberResourceEnvironment

import java.util.Locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/** What Compose Resources reads inside the device, which `stringResource` and the others choose a `values-` folder from. */
class LanguageEnvironmentTest {
    private fun environmentIn(configuration: DeviceConfiguration): ResourceEnvironment {
        var environment: ResourceEnvironment? = null
        renderComposeScene(402, 874) {
            DeviceFrameView(DevicePresets.iPhone16Pro, configuration = configuration.copy(isFrameVisible = false)) {
                environment = rememberResourceEnvironment()
            }
        }
        return assertNotNull(environment)
    }

    private fun environmentOutsideADevice(): ResourceEnvironment {
        var environment: ResourceEnvironment? = null
        renderComposeScene(402, 874) {
            environment = rememberResourceEnvironment()
        }
        return assertNotNull(environment)
    }

    @Test
    fun theContentReadsResourcesInTheLanguageOfTheConfiguration() {
        val welsh = environmentIn(DeviceConfiguration(language = "cy"))
        assertEquals(welsh, environmentIn(DeviceConfiguration(language = "cy")))
        assertNotEquals(welsh, environmentIn(DeviceConfiguration(language = "pt")))
    }

    @Test
    fun aRegionIsPartOfTheLanguage() {
        assertNotEquals(
            environmentIn(DeviceConfiguration(language = "pt")),
            environmentIn(DeviceConfiguration(language = "pt-BR")),
        )
    }

    @Test
    fun withoutALanguageTheContentReadsTheHostsOwn() {
        val host = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("cy"))
        try {
            assertEquals(environmentIn(DeviceConfiguration(language = "cy")), environmentIn(DeviceConfiguration()))
            assertNotEquals(environmentIn(DeviceConfiguration(language = "pt")), environmentIn(DeviceConfiguration()))
        } finally {
            Locale.setDefault(host)
        }
    }

    @Test
    fun theContentStillReadsTheDevicesDarkModeWhenItHasALanguage() {
        assertNotEquals(
            environmentIn(DeviceConfiguration(language = "cy", appearance = Appearance.Light)),
            environmentIn(DeviceConfiguration(language = "cy", appearance = Appearance.Dark)),
        )
    }

    @Test
    fun theLanguageIsOnlyTheDevicesNotTheHosts() {
        var outside: ResourceEnvironment? = null
        renderComposeScene(402, 874) {
            outside = rememberResourceEnvironment()
            DeviceFrameView(DevicePresets.iPhone16Pro, configuration = DeviceConfiguration(language = "pt")) {}
        }
        assertEquals(environmentOutsideADevice(), outside)
    }
}
