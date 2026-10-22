package com.apadmi.cmpdevicepreview.preview

internal val MockApi = PanelSetting.toggle(key = "mockApi", title = "Mock API", note = "Serve canned responses")

internal val Environment = PanelSetting.choice(key = "env", title = "Environment", options = listOf("Dev", "Staging", "São Paulo"))
