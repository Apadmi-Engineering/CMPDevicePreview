package com.apadmi.cmpdevicepreview

/** Whether this is Android or iOS, where focusing a text field raises the on-screen keyboard. */
internal expect val isMobilePlatform: Boolean

// The web reports a wheel's movement in pixels, the others in notches.
internal expect val isWheelDeltaInPixels: Boolean
