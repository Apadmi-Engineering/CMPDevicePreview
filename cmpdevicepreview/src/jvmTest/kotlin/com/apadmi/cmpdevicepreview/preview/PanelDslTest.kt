package com.apadmi.cmpdevicepreview.preview

import androidx.compose.material3.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PanelDslTest {
    private val usualOrder = listOf(
        PanelControl.Frame,
        PanelControl.Orientation,
        PanelControl.Appearance,
        PanelControl.TextSize,
        PanelControl.DisplaySize,
        PanelControl.LayoutDirection,
        PanelControl.Language,
        PanelControl.Bezel,
        PanelControl.SystemUi,
        PanelControl.Keyboard,
        PanelControl.Backdrop,
        PanelControl.DotGrid,
    )

    // The panel's own controls in the order they are drawn, sections and all.
    private fun List<PanelEntry>.controls(): List<PanelControl> = flatMap { entry ->
        when (entry) {
            is PanelEntry.Control -> listOf(entry.control)
            is PanelEntry.Section -> entry.entries.controls()
            else -> emptyList()
        }
    }

    private fun List<PanelEntry>.sectionTitles(): List<String> = filterIsInstance<PanelEntry.Section>().map { it.title }

    @Test
    fun theDefaultPanelHasEveryControlInItsUsualOrder() {
        val panel = buildPanel(DefaultPanel)
        assertEquals(usualOrder, panel.entries.controls())
        assertEquals(listOf("Text & display", "Display"), panel.entries.sectionTitles())
        assertEquals(PanelControl.entries.toSet(), panel.controls)
        assertEquals(DefaultPanelTitle, assertNotNull(panel.header).title)
    }

    @Test
    fun aControlHiddenInTheDefaultsIsLeftOut() {
        val panel = buildPanel {
            defaults {
                backdrop.isVisible = false
                header.isVisible = false
                previewMode.isVisible = false
            }
        }
        assertEquals(usualOrder - PanelControl.Backdrop, panel.entries.controls())
        assertFalse(PanelControl.Backdrop in panel.controls)
        assertFalse(PanelControl.PreviewMode in panel.controls)
        assertNull(panel.header)
    }

    @Test
    fun aDefaultSectionWithEveryControlHiddenIsLeftOut() {
        val panel = buildPanel {
            defaults {
                bezel.isVisible = false
                systemUi.isVisible = false
                keyboard.isVisible = false
            }
        }
        assertEquals(listOf("Text & display"), panel.entries.sectionTitles())
    }

    @Test
    fun controlsAreInTheOrderTheyAreWrittenAndNothingElseIsShown() {
        val panel = buildPanel {
            appearance()
            section("Mine") {
                keyboard()
                textSize()
            }
            frame()
        }
        assertEquals(listOf(PanelControl.Appearance, PanelControl.Keyboard, PanelControl.TextSize, PanelControl.Frame), panel.entries.controls())
        assertEquals(setOf(PanelControl.Appearance, PanelControl.Keyboard, PanelControl.TextSize, PanelControl.Frame), panel.controls)
        assertNull(panel.header)
    }

    @Test
    fun aControlPlacedOutsideTheDefaultsIsTakenOutOfThem() {
        val panel = buildPanel {
            language()
            defaults()
            header(title = "Acme Preview")
        }
        assertEquals(listOf(PanelControl.Language) + (usualOrder - PanelControl.Language), panel.entries.controls())
        assertEquals("Acme Preview", assertNotNull(panel.header).title)
    }

    @Test
    fun itemsAndSettingsGoWhereTheyAreWritten() {
        val panel = buildPanel {
            frame()
            item { Text("Mine") }
            section("Debug") { setting(MockApi) }
            setting(Environment)
            appearance()
        }
        val kinds = panel.entries.map { it::class }
        assertEquals(
            listOf(PanelEntry.Control::class, PanelEntry.Item::class, PanelEntry.Section::class, PanelEntry.Setting::class, PanelEntry.Control::class),
            kinds,
        )
        assertEquals(listOf(MockApi, Environment), panel.settings)
    }

    @Test
    fun theTextSizeRangeIsTheSizesWithinIt() {
        val placed = buildPanel { textSize(range = 0.85f..2f) }
        assertEquals(0.85f, placed.textSizeSteps.first().scale)
        assertEquals(2f, placed.textSizeSteps.last().scale)

        val inTheDefaults = buildPanel { defaults { textSize.range = 1f..1.5f } }
        assertEquals(listOf(1f, 1.05f, 1.1f, 1.15f, 1.2f, 1.25f, 1.3f, 1.35f, 1.4f, 1.45f, 1.5f), inTheDefaults.textSizeSteps.map { it.scale })
    }

    @Test
    fun placingAControlTwiceIsAnError() {
        assertFailsWith<IllegalArgumentException> {
            buildPanel {
                frame()
                frame()
            }
        }
        assertFailsWith<IllegalArgumentException> {
            buildPanel {
                keyboard()
                section("Again") { keyboard() }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            buildPanel {
                previewMode()
                previewMode()
            }
        }
    }

    @Test
    fun callingDefaultsTwiceIsAnError() {
        assertFailsWith<IllegalArgumentException> {
            buildPanel {
                defaults()
                defaults()
            }
        }
    }

    @Test
    fun twoSettingsWithTheSameKeyAreAnError() {
        val sameKey = PanelSetting.toggle(key = MockApi.key, title = "Other")
        assertFailsWith<IllegalArgumentException> {
            buildPanel {
                setting(MockApi)
                setting(sameKey)
            }
        }
    }

    @Test
    fun aTextSizeRangeWithNoSizesIsAnError() {
        assertFailsWith<IllegalArgumentException> { buildPanel { textSize(range = 3.1f..4f) } }
        assertFailsWith<IllegalArgumentException> { buildPanel { defaults { textSize.range = 1.01f..1.04f } } }
    }

    @Test
    fun anOptionInTheDefaultsForAControlPlacedElsewhereIsAnError() {
        assertFailsWith<IllegalArgumentException> {
            buildPanel {
                textSize()
                defaults { textSize.range = 1f..2f }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            buildPanel {
                defaults { backdrop.isVisible = false }
                backdrop()
            }
        }
    }

    @Test
    fun aSettingALinkCannotCarryIsAnError() {
        assertFailsWith<IllegalArgumentException> { PanelSetting.toggle(key = "mock api", title = "Mock API") }
        assertFailsWith<IllegalArgumentException> { PanelSetting.choice(key = "env", title = "Environment", options = emptyList(), default = "Dev") }
        assertFailsWith<IllegalArgumentException> { PanelSetting.choice(key = "env", title = "Environment", options = listOf("Dev"), default = "Prod") }
        assertFailsWith<IllegalArgumentException> { PanelSetting.choice(key = "env", title = "Environment", options = listOf("Dev", "DEV")) }
    }

    @Test
    fun aSettingsValueIsItsDefaultUntilItIsChanged() {
        val values = PanelSettingValues()
        assertFalse(values[MockApi])
        assertEquals("Dev", values[Environment])

        val changed = values.with(MockApi, true).with(Environment, "São Paulo")
        assertTrue(changed[MockApi])
        assertEquals("São Paulo", changed[Environment])
        assertEquals(values, changed.with(MockApi, false).with(Environment, "Dev"))
    }
}
