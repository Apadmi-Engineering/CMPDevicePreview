package com.apadmi.cmpdevicepreview.sample

import com.apadmi.cmpdevicepreview.preview.PanelScope
import com.apadmi.cmpdevicepreview.preview.PanelSetting

// A setting of the app's own, beside the preview's, to show the Saved tab's empty state without unsaving each place.
val NoSavedPlaces = PanelSetting.toggle(key = "noSaved", title = "No saved places", note = "Shows the Saved tab's empty state")

val samplePanel: PanelScope.() -> Unit = {
    defaults()
    section("Sample app") {
        setting(NoSavedPlaces)
    }
}
