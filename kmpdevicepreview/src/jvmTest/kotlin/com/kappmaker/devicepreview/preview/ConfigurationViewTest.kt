package com.kappmaker.devicepreview.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.unit.dp
import com.kappmaker.devicepreview.core.SimulatedDevice
import com.kappmaker.devicepreview.devices.DevicePresets
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun pixelRatiosDropTrailingZeros() {
        assertEquals("3x", formatPixelRatio(3f))
        assertEquals("2.625x", formatPixelRatio(2.625f))
        assertEquals("2.75x", formatPixelRatio(2.75f))
        assertEquals("1.5x", formatPixelRatio(1.5f))
    }
}
