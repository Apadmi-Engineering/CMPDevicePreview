package com.apadmi.cmpdevicepreview.sample

import com.apadmi.cmpdevicepreview.preview.PanelScope
import com.apadmi.cmpdevicepreview.preview.PanelSetting

// A feature flag of the app's own, switched from the panel beside the preview's settings.
val ShowOfferBanner = PanelSetting.toggle(key = "offerBanner", title = "Offer banner", note = "Shows a summer sale on Explore")

val samplePanel: PanelScope.() -> Unit = {
    defaults()
    section("Sample app") {
        setting(ShowOfferBanner)
    }
}
