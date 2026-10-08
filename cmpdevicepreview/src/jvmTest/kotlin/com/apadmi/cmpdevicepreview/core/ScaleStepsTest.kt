package com.apadmi.cmpdevicepreview.core

import androidx.compose.ui.geometry.Size
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ScaleStepsTest {

    @Test
    fun displaySizesStopWhereTheShorterSideIs320Dp() {
        // 360 dp wide: room for one larger size.
        val galaxy = DevicePresets.galaxyS25.displayScaleSteps
        assertEquals(listOf("Small", "Default", "Large"), galaxy.map { it.label })
        assertEquals(listOf(0.85f, 1f, 1.125f), galaxy.map { it.scale })

        // 411 dp wide: three, the largest leaving 320 dp.
        val pixel = DevicePresets.pixel9.displayScaleSteps
        assertEquals(listOf("Small", "Default", "Large", "Larger", "Largest"), pixel.map { it.label })
        assertEquals(320f, DevicePresets.pixel9.portraitScreenSize.width / pixel.last().scale, 0.01f)

        // Never more than half as large again, however wide the screen.
        assertEquals(1.5f, DevicePresets.largeDesktopWindow.displayScaleSteps.last().scale, 0.001f)
        // A window narrower than 320 dp can only get smaller.
        assertEquals(listOf(0.85f, 1f), FreeformDevice(Size(300f, 600f)).displayScaleSteps.map { it.scale })
    }

    @Test
    fun textSizesGoWellBeyondWhatDevicesOffer() {
        assertEquals(51, FontScaleSteps.size)
        assertEquals(0.5f, FontScaleSteps.first().scale)
        assertEquals(3f, FontScaleSteps.last().scale)
        assertEquals(listOf("95%", "Default", "105%"), FontScaleSteps.slice(9..11).map { it.label })
        assertEquals("120%", FontScaleSteps.single { it.scale == 1.2f }.label)
    }

    @Test
    fun theNearestOfTwoAsNearIsTheOneNearerTheDefault() {
        val steps = listOf(ScaleStep("a", 1f), ScaleStep("b", 1.1f), ScaleStep("c", 1.2f))
        assertEquals(1.1f, steps.nearest(1.15f).scale)
        assertEquals(1.2f, steps.nearest(1.3f).scale)
        assertEquals(1.3f, FontScaleSteps.nearest(1.31f).scale)
    }

    @Test
    fun changingDeviceKeepsTheTextSizeAndTheNearestDisplaySize() {
        val pixel = SimulatedDevice(
            DevicePresets.pixel9,
            DeviceConfiguration(fontScale = 1.15f, displayScale = DevicePresets.pixel9.displayScaleSteps.last().scale),
        )
        val galaxy = pixel.copy(device = DevicePresets.galaxyS25).adaptedToDevice()
        assertEquals(1.15f, galaxy.configuration.fontScale)
        assertEquals(1.125f, galaxy.configuration.displayScale)

        // Every device offers the same text sizes, so even one set in code is kept.
        val iPhone = pixel.copy(
            device = DevicePresets.iPhone16Pro,
            configuration = pixel.configuration.copy(fontScale = 1.33f),
        ).adaptedToDevice()
        assertEquals(1.33f, iPhone.configuration.fontScale)

        // A configuration the device can show is kept as it is.
        val default = SimulatedDevice(DevicePresets.pixel9)
        assertSame(default, default.adaptedToDevice())
    }
}
