package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.apadmi.cmpdevicepreview.core.DeviceConfiguration
import com.apadmi.cmpdevicepreview.core.FreeformDevice
import com.apadmi.cmpdevicepreview.core.SimulatedDevice
import com.apadmi.cmpdevicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ConfigurationViewTest {

    @Test
    fun yourOwnItemsAreShownInOrderAsWideAsTheSettings() {
        val shown = mutableListOf<String>()
        var width = -1
        var primary = Color.Unspecified
        renderComposeScene(300, 4000) {
            ConfigurationView(
                modifier = Modifier.width(300.dp).fillMaxSize(),
                simulatedDevice = SimulatedDevice(DevicePresets.pixel9),
                onChangeConfiguration = {},
                panelItems = listOf(
                    {
                        shown += "first"
                        primary = MaterialTheme.colorScheme.primary
                        Box(Modifier.fillMaxWidth().onSizeChanged { width = it.width })
                    },
                    { shown += "second" },
                ),
            )
        }
        assertEquals(listOf("first", "second"), shown)
        // The panel's 16 dp margins, as its settings have.
        assertEquals(300 - 2 * 16, width)
        assertEquals(PanelColors.Accent, primary, "inside the panel's theme")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aTypedSizeIsFittedToTheDevice() = runComposeUiTest {
        var changed: SimulatedDevice? = null
        setContent {
            ConfigurationView(
                modifier = Modifier.width(300.dp).fillMaxSize(),
                simulatedDevice = SimulatedDevice(FreeformDevice(Size(800f, 800f)), DeviceConfiguration(displayScale = 1.5f)),
                onChangeConfiguration = { changed = it },
            )
        }
        val width = onAllNodes(hasSetTextAction())[0]
        width.performTextReplacement("300")
        width.performImeAction()

        val resized = assertNotNull(changed)
        assertEquals(FreeformDevice(Size(300f, 800f)), resized.device)
        // A window 300 dp across offers no display size larger than the default.
        assertEquals(1f, resized.configuration.displayScale)
    }

    @Test
    fun screenshotNamesAreSafeFileNames() {
        val image = ImageBitmap(2064, 2752)
        assertEquals("iPad Pro 13 (M5) 2064x2752.png", screenshotName(DevicePresets.iPadPro13M5, image, includesFrame = false))
        assertEquals("iPad Pro 13 (M5) with frame.png", screenshotName(DevicePresets.iPadPro13M5, image, includesFrame = true))
    }

    @Test
    fun pixelRatiosDropTrailingZeros() {
        assertEquals("3x", formatPixelRatio(3f))
        assertEquals("2.625x", formatPixelRatio(2.625f))
        assertEquals("2.75x", formatPixelRatio(2.75f))
        assertEquals("1.5x", formatPixelRatio(1.5f))
    }
}
