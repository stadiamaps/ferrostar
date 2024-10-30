package com.stadiamaps.ferrostar.core.annotation.valhalla

import com.squareup.moshi.Json
import com.stadiamaps.ferrostar.core.annotation.Speed

data class ValhallaOSRMExtendedAnnotation(
    @Json(name = "maxspeed") val speedLimit: Speed?,
    val speed: Double?,
    val distance: Double?,
    val duration: Double?
)
