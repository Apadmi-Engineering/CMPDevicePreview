package com.kappmaker.devicepreview.frame

/**
 * Runs [parse], or reports the failure and returns null: malformed artwork never breaks the
 * previewed content, that part of the device is simply not drawn.
 */
internal fun <T : Any> reportingParseFailure(what: String, parse: () -> T): T? = try {
    parse()
} catch (error: Exception) {
    println("DevicePreview: could not parse the $what, it will not be drawn: $error")
    null
}
