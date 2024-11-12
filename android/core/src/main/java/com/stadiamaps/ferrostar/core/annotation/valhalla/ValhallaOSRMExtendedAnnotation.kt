package com.stadiamaps.ferrostar.core.annotation.valhalla

import com.squareup.moshi.Json
import com.stadiamaps.ferrostar.core.annotation.Speed

data class ValhallaOSRMExtendedAnnotation(
    // The speed limit of the segment.
    @Json(name = "maxspeed") val speedLimit: Speed?,
    // The estimated speed of travel for the segment, in meters per second.
    val speed: Double?,
    // The distance in meters of the segment.
    val distance: Double?,
    // The estimated time to traverse the segment, in seconds.
    val duration: Double?
)
