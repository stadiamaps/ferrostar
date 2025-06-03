package com.stadiamaps.ferrostar.core.annotation.valhalla

import com.stadiamaps.ferrostar.core.annotation.Speed
import com.stadiamaps.ferrostar.core.annotation.SpeedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValhallaOSRMExtendedAnnotation(
    /** The speed limit of the segment. */
    @SerialName("maxspeed") @Serializable(with = SpeedSerializer::class) val speedLimit: Speed?,
    /** The estimated speed of travel for the segment, in meters per second. */
    val speed: Double?,
    /** The distance in meters of the segment. */
    val distance: Double?,
    /** The estimated time to traverse the segment, in seconds. */
    val duration: Double?
)
