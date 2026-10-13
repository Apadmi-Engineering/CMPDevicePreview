package com.apadmi.cmpdevicepreview.preview

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.apadmi.cmpdevicepreview.core.Device
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DevicePickerTest {

    private val freeform = FreeformDevice(Size(400f, 800f))

    private fun ComposeUiTest.showPicker(selected: Device = DevicePresets.pixel9, onPick: (Device) -> Unit = {}) {
        setContent { DevicePickerDialog(selected = selected, freeform = freeform, onPick = onPick, onDismiss = {}) }
    }

    @Test
    fun searchingNarrowsTheList() = runComposeUiTest {
        showPicker()
        onNode(hasSetTextAction()).performTextInput("galaxy s2")
        onNodeWithText("Galaxy S25").assertExists()
        onNodeWithText("Galaxy S24").assertExists()
        onNodeWithText("Pixel 9").assertDoesNotExist()
        onNodeWithText("Freeform").assertDoesNotExist()
    }

    @Test
    fun aKindFiltersTheListAndLeavesOutFreeform() = runComposeUiTest {
        showPicker()
        onNode(hasText("Tablets") and isSelectable()).performClick()
        onNodeWithText("Galaxy Tab S11").assertExists()
        onNodeWithText("Pixel 9").assertDoesNotExist()
        onNodeWithText("Freeform").assertDoesNotExist()
    }

    @Test
    fun freeformIsFoundByWhatItDoes() = runComposeUiTest {
        var picked: Device? = null
        showPicker(onPick = { picked = it })
        onNode(hasSetTextAction()).performTextInput("resize")
        onNodeWithText("Freeform").performClick()
        assertEquals(freeform, picked)
    }

    @Test
    fun theSelectedDeviceIsInViewWhenThePickerOpens() = runComposeUiTest {
        // The last row of the list.
        showPicker(selected = DevicePresets.smallDesktopWindow)
        onNode(hasText("Small Desktop Window") and isSelected()).assertIsDisplayed()
    }
}
