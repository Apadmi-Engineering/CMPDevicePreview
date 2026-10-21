package com.apadmi.cmpdevicepreview.core

/**
 * A language the panel offers for the content inside the device.
 *
 * @property tag The language's BCP 47 tag, such as `en`, `cy` or `pt-BR`. It is what
 * [DeviceConfiguration.language] holds, and what Compose Resources matches `values-` folders against.
 * @property name What the panel shows for it. The language's own name suits best, such as "Cymraeg".
 */
public data class PreviewLanguage(
    val tag: String,
    val name: String,
)
