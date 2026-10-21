package com.apadmi.cmpdevicepreview.sample

import com.apadmi.cmpdevicepreview.sample.resources.Res
import com.apadmi.cmpdevicepreview.sample.resources.transport_bicycle
import com.apadmi.cmpdevicepreview.sample.resources.transport_car
import com.apadmi.cmpdevicepreview.sample.resources.transport_coach
import com.apadmi.cmpdevicepreview.sample.resources.transport_plane
import com.apadmi.cmpdevicepreview.sample.resources.transport_train
import org.jetbrains.compose.resources.StringResource

/**
 * A way of travelling, which the profile's transport preference is picked from.
 *
 * @property label The name shown for it.
 */
enum class Transport(val label: StringResource) {
    Train(Res.string.transport_train),
    Plane(Res.string.transport_plane),
    Car(Res.string.transport_car),
    Coach(Res.string.transport_coach),
    Bicycle(Res.string.transport_bicycle),
    ;
}
