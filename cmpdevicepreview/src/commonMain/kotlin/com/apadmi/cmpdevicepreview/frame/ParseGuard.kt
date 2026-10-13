package com.apadmi.cmpdevicepreview.frame

/**
 * Runs [parse], or reports the failure and returns null, so malformed artwork is left undrawn.
 *
 * @param what What is being parsed, named in the failure report, e.g. `device frame body artwork`.
 * @param parse Parses the artwork; an exception it throws counts as a failure.
 * @return The result of [parse], or null when it failed.
 */
internal fun <T : Any> reportingParseFailure(what: String, parse: () -> T): T? = try {
    parse()
} catch (error: Exception) {
    println("CmpDevicePreview: could not parse the $what, it will not be drawn: $error")
    null
}
