package com.apadmi.devicepreview.core

import androidx.compose.ui.geometry.Size
import com.apadmi.devicepreview.devices.DevicePresets
import com.apadmi.devicepreview.devices.android.Pixel6
import com.apadmi.devicepreview.devices.other.GenericDevice
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

    @Test
    fun aFreeformWindowHasItsOwnSizeAndCannotBeTurned() {
        val freeform = FreeformDevice(Size(834f, 1112f))
        assertEquals("Freeform", freeform.name)
        assertEquals(DeviceType.OTHER, freeform.type)
        assertFalse(freeform.isRotatable)
        assertFalse(freeform.isTouchscreen)
        assertEquals(Size(834f, 1112f), freeform.portraitScreenSize)

        // Asking for landscape changes nothing: the window is the shape it was resized to.
        val upright = SimulatedDevice(freeform).currentScreenSize
        val asked = SimulatedDevice(freeform, DeviceConfiguration(isPortrait = false)).currentScreenSize
        assertEquals(upright, asked)
        assertEquals(834f, asked.width)
        assertEquals(1112f, asked.height)

        // It is a plain window: no insets, no status bar, no keyboard.
        val simulated = SimulatedDevice(freeform, DeviceConfiguration(isKeyboardVisible = true))
        assertEquals(DeviceInsets.Zero, simulated.currentSafeArea)
        assertNull(simulated.currentKeyboardHeight)
        assertNull(freeform.systemUi)
        assertNull(freeform.frame)
    }

    @Test
    fun aRaisedKeyboardIsDroppedWhenTheDeviceHasNone() {
        val raised = DeviceConfiguration(isKeyboardVisible = true)
        val phone = SimulatedDevice(DevicePresets.iPhone16Pro, raised)
        assertEquals(phone, phone.adaptedToDevice())

        val freeform = phone.copy(device = FreeformDevice(Size(400f, 800f)))
        assertFalse(freeform.adaptedToDevice().configuration.isKeyboardVisible)
        // Everything else about the configuration is kept.
        assertEquals(freeform.configuration.copy(isKeyboardVisible = false), freeform.adaptedToDevice().configuration)
    }
}
