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
    fun aDesktopWindowCannotBeTurned() {
        val desktop = DevicePresets.largeDesktopWindow
        assertFalse(desktop.isRotatable)
        assertTrue(desktop.laidOutInPortrait(isPortrait = false))

        // Asking for landscape changes nothing: the window keeps its natural, wide shape.
        val natural = SimulatedDevice(desktop).currentScreenSize
        val asked = SimulatedDevice(desktop, DeviceConfiguration(isPortrait = false)).currentScreenSize
        assertEquals(natural, asked)
        assertTrue(asked.width > asked.height, "a desktop window is wider than tall")

        // Everything else still turns.
        for (device in listOf(DevicePresets.pixel9, DevicePresets.galaxyTabS11, DevicePresets.pixel10ProFold)) {
            assertTrue(device.isRotatable, device.name)
            val upright = SimulatedDevice(device).currentScreenSize
            val turned = SimulatedDevice(device, DeviceConfiguration(isPortrait = false)).currentScreenSize
            assertEquals(upright.rotated(), turned, device.name)
        }
        // A device of unknown kind can be turned.
        assertTrue(GenericDevice().isRotatable)
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
