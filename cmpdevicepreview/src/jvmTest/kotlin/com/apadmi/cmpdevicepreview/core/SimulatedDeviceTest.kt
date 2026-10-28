package com.apadmi.cmpdevicepreview.core

import androidx.compose.ui.geometry.Size
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SimulatedDeviceTest {
    private val languages = listOf(PreviewLanguage("en", "English"), PreviewLanguage("cy", "Cymraeg"), PreviewLanguage("pt-BR", "Português"))

    private fun inLanguages(language: String?, offered: List<PreviewLanguage> = languages) =
        SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(language = language)).inLanguages(offered).configuration.language

    @Test
    fun aLanguageTheHostOffersIsKept() {
        assertEquals("cy", inLanguages("cy"))
    }

    @Test
    fun aLanguageIsMatchedIgnoringCaseAndComesBackAsTheHostWroteIt() {
        assertEquals("pt-BR", inLanguages("PT-br"))
    }

    @Test
    fun noLanguageOrOneThatIsNotOfferedIsTheFirstOffered() {
        assertEquals("en", inLanguages(null))
        assertEquals("en", inLanguages("fr"))
    }

    @Test
    fun withoutLanguagesTheHostsOwnIsKeptWhateverWasChosen() {
        assertNull(inLanguages("cy", offered = emptyList()))
    }

    @Test
    fun fittingTheLanguageLeavesAnUnchangedDeviceAsItIs() {
        val device = SimulatedDevice(DevicePresets.pixel9, DeviceConfiguration(language = "cy"))
        assertSame(device, device.inLanguages(languages))
    }

    @Test
    fun theKeyboardHeightFollowsTheOrientation() {
        val portrait = SimulatedDevice(DevicePresets.iPhone16Pro)
        assertEquals(335f, portrait.currentKeyboardHeight)
        val landscape = portrait.copy(configuration = DeviceConfiguration(orientation = DeviceOrientation.Landscape))
        assertEquals(208f, landscape.currentKeyboardHeight)
        assertNull(SimulatedDevice(DevicePresets.largeDesktopWindow).currentKeyboardHeight)
    }

    @Test
    fun aDesktopWindowCannotBeTurned() {
        val desktop = DevicePresets.largeDesktopWindow
        assertFalse(desktop.isRotatable)
        assertTrue(desktop.isLaidOutInPortrait(DeviceOrientation.Landscape))

        // Asking for landscape changes nothing: the window keeps its natural, wide shape.
        val natural = SimulatedDevice(desktop).currentSize
        val asked = SimulatedDevice(desktop, DeviceConfiguration(orientation = DeviceOrientation.Landscape)).currentSize
        assertEquals(natural, asked)
        assertTrue(asked.width > asked.height, "a desktop window is wider than tall")
    }

    @Test
    fun phonesTabletsAndFoldablesTurn() {
        for (device in listOf(DevicePresets.pixel9, DevicePresets.galaxyTabS11, DevicePresets.pixel10ProFold)) {
            assertTrue(device.isRotatable, device.name)
            val upright = SimulatedDevice(device).currentSize
            val turned = SimulatedDevice(device, DeviceConfiguration(orientation = DeviceOrientation.Landscape)).currentSize
            assertEquals(Size(upright.height, upright.width), turned, device.name)
        }
    }

    @Test
    fun aDeviceThatDoesNotGiveItsKindCanBeTurned() {
        assertTrue(CustomDevice(DeviceType.Other).isRotatable)
    }

    @Test
    fun theSafeAreaDoesNotIncludeTheKeyboard() {
        val device = SimulatedDevice(DevicePresets.iPhone16Pro, DeviceConfiguration(isKeyboardVisible = true))
        assertEquals(DeviceInsets(top = 62f, bottom = 34f), device.currentSafeArea)
    }

    @Test
    fun phonesTabletsAndFoldablesTakeTheMouseAsAFingerByDefault() {
        assertTrue(DevicePresets.pixel9.isMouseAsTouchByDefault)
        assertTrue(DevicePresets.galaxyTabS11.isMouseAsTouchByDefault)
        assertTrue(DevicePresets.pixel10ProFold.isMouseAsTouchByDefault)
        assertFalse(DevicePresets.largeDesktopWindow.isMouseAsTouchByDefault)
        // A device that doesn't give its kind is a phone, whatever its platform.
        assertTrue(CustomDevice(DeviceType.Android).isMouseAsTouchByDefault)
        assertTrue(CustomDevice(DeviceType.Other).isMouseAsTouchByDefault)
    }

    @Test
    fun aFreeformWindowHasItsOwnSizeAndCannotBeTurned() {
        val freeform = FreeformDevice(Size(834f, 1112f))
        assertEquals("Freeform", freeform.name)
        assertEquals(DeviceType.Other, freeform.type)
        assertFalse(freeform.isRotatable)
        assertTrue(freeform.isMouseAsTouchByDefault)
        assertEquals(Size(834f, 1112f), freeform.portraitSize)
        assertNull(freeform.devicePixelRatio)

        // Asking for landscape changes nothing: the window is the shape it was resized to.
        val upright = SimulatedDevice(freeform).currentSize
        val asked = SimulatedDevice(freeform, DeviceConfiguration(orientation = DeviceOrientation.Landscape)).currentSize
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
    /** A device of your own, which only says what it has to. */
    private class CustomDevice(override val type: DeviceType) : Device {
        override val name: String = "Custom"
        override val portraitSize: Size = Size(400f, 800f)
        override val devicePixelRatio: Float = 2f
    }
}
