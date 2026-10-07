package com.kappmaker.devicepreview.core

import com.kappmaker.devicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SimulatedDeviceTest {

    @Test
    fun theKeyboardHeightFollowsTheOrientation() {
        val portrait = SimulatedDevice(DevicePresets.iPhone16Pro)
        assertEquals(335f, portrait.currentKeyboardHeight)
        val landscape = portrait.copy(configuration = DeviceConfiguration(isPortrait = false))
        assertEquals(208f, landscape.currentKeyboardHeight)
        assertNull(SimulatedDevice(DevicePresets.largeDesktopWindow).currentKeyboardHeight)
    }

    @Test
    fun theSafeAreaDoesNotIncludeTheKeyboard() {
        val device = SimulatedDevice(DevicePresets.iPhone16Pro, DeviceConfiguration(isKeyboardVisible = true))
        assertEquals(DeviceInsets(top = 62f, bottom = 34f), device.currentSafeArea)
    }
}
