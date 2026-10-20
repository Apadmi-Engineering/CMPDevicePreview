package com.apadmi.cmpdevicepreview.preview

import com.apadmi.cmpdevicepreview.core.PreviewSettings
import com.apadmi.cmpdevicepreview.core.SimulatedDevice

/**
 * What the URL and the stored settings keep: the device with its configuration, and the preview's settings.
 *
 * @property simulatedDevice The device and how it is shown.
 * @property previewSettings How the preview around the device is shown.
 */
internal data class PreviewState(val simulatedDevice: SimulatedDevice, val previewSettings: PreviewSettings)
