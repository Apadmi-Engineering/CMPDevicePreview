package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import kotlin.test.assertSame

class PreviewSaversTest {
    private fun <T : Any> Saver<T, Any>.roundTrip(value: T): T {
        val saved = assertNotNull(with(this) { SaverScope(::isBundleable).save(value) })
        return assertNotNull(restore(saved))
    }

    @Test
    fun aPresetAndEveryConfigurationSettingAreRestored() {
        val pixel = DevicePresets.pixel9
        val simulated = SimulatedDevice(
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
            ),
        )
        assertEquals(simulated, simulatedDeviceSaver(initialDevice = DevicePresets.galaxyS25).roundTrip(simulated))
    }

    @Test
    fun aFreeformWindowIsRestoredAtItsSize() {
        val simulated = SimulatedDevice(FreeformDevice(Size(834f, 1112f)))
        assertEquals(simulated, simulatedDeviceSaver(initialDevice = DevicePresets.galaxyS25).roundTrip(simulated))
    }

    @Test
    fun aCustomInitialDeviceIsRestoredAsItself() {
        val custom = CustomDevice("Mine")
        val restored = simulatedDeviceSaver(initialDevice = custom).roundTrip(SimulatedDevice(custom))
        assertSame(custom, restored.device)
    }

    @Test
    fun anyOtherDeviceIsRestoredAsTheInitialOne() {
        val restored = simulatedDeviceSaver(initialDevice = DevicePresets.pixel9).roundTrip(SimulatedDevice(CustomDevice("Other")))
        assertSame(DevicePresets.pixel9, restored.device)
    }

    @Test
    fun previewSettingsAreRestored() {
        val settings = PreviewSettings(backdrop = Backdrop.Auto, isDotGridVisible = false, isPreviewModeEnabled = false)
        assertEquals(settings, PreviewSettingsSaver.roundTrip(settings))
    }

    // StateRestorationTester isn't implemented outside Android, so this saves and restores through a
    // registry of its own, as an Activity being recreated would.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun thePreviewKeepsItsSettingsWhenItsStateIsRestored() = runComposeUiTest {
        var registry by mutableStateOf(SaveableStateRegistry(restoredValues = null, canBeSaved = ::isBundleable))
        var isShown by mutableStateOf(true)
        setContent {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                if (isShown) {
                    Box(Modifier.width(1000.dp).height(800.dp)) {
                        DeviceWithConfigurationView(initialDevice = DevicePresets.pixel9, isPersistenceEnabled = false) {}
                    }
                }
            }
        }
        onNode(hasText("Landscape") and isSelectable()).performClick()
        onNode(hasText("Auto") and isSelectable()).performScrollTo().performClick()
        waitForIdle()

        val saved = registry.performSave()
        isShown = false
        waitForIdle()
        registry = SaveableStateRegistry(restoredValues = saved, canBeSaved = ::isBundleable)
        isShown = true
        waitForIdle()

        onNode(hasText("Landscape") and isSelectable()).assertIsSelected()
        onNode(hasText("Auto") and isSelectable()).performScrollTo().assertIsSelected()
    }

    /** Whether [value] is made only of types an Android Bundle holds, as Android's registry requires. */
    private fun isBundleable(value: Any): Boolean = when (value) {
        is SnapshotMutableState<*> -> value.value.let { it == null || isBundleable(it) }
        is List<*> -> value.all { it == null || isBundleable(it) }
        else -> value is String || value is Boolean || value is Float || value is Int
    }
    /** A device of your own, which the saver can only restore as the initial device. */
    private class CustomDevice(override val name: String) : Device {
        override val type: DeviceType = DeviceType.Other
        override val portraitSize: Size = Size(400f, 800f)
        override val devicePixelRatio: Float = 2f
    }
}
