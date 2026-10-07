package com.kappmaker.devicepreview.core

import com.kappmaker.devicepreview.devices.DevicePresets
import com.kappmaker.devicepreview.devices.android.Pixel6
import com.kappmaker.devicepreview.devices.other.GenericDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun phonesTabletsAndFoldablesAreTouchscreens() {
        assertTrue(DevicePresets.pixel9.isTouchscreen)
        assertTrue(DevicePresets.galaxyTabS11.isTouchscreen)
        assertTrue(DevicePresets.pixel10ProFold.isTouchscreen)
        assertFalse(DevicePresets.largeDesktopWindow.isTouchscreen)
        // Without a kind, the platform decides.
        assertTrue(Pixel6().isTouchscreen)
        assertFalse(GenericDevice().isTouchscreen)
    }
}
