package com.apadmi.cmpdevicepreview.frame

/** Runs [parse], or reports the failure and returns null, so malformed artwork is left undrawn. */
internal fun <T : Any> reportingParseFailure(what: String, parse: () -> T): T? = try {
    parse()
} catch (error: Exception) {
    println("CmpDevicePreview: could not parse the $what, it will not be drawn: $error")
    null
}
