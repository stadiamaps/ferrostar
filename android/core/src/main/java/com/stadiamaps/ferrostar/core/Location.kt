package com.stadiamaps.ferrostar.core

import uniffi.ferrostar.CourseOverGround
import uniffi.ferrostar.GeographicCoordinates

interface Location {
    val coordinates: GeographicCoordinates
    val horizontalAccuracy: Float
    val courseOverGround: CourseOverGround?
}

data class SimulatedLocation(
    override val coordinates: GeographicCoordinates,
    override val horizontalAccuracy: Float,
    override val courseOverGround: CourseOverGround?
) : Location

data class AndroidLocation(
    override val coordinates: GeographicCoordinates,
    override val horizontalAccuracy: Float,
    override val courseOverGround: CourseOverGround?
) : Location {
    constructor(location: android.location.Location) : this(
        GeographicCoordinates(location.latitude, location.longitude),
        location.accuracy,
        if (location.hasBearing() && location.hasBearingAccuracy()) {
            CourseOverGround(
                location.bearing.toInt().toUShort(),
                location.bearingAccuracyDegrees.toInt().toUShort()
            )
        } else {
            null
        }
    )
}

interface LocationProvider {
    val lastLocation: Location?
    val lastHeading: Float?

    fun startUpdating()
    fun stopUpdating()
}

interface LocationUpdateListener {
    fun onLocationUpdated(location: Location)
    fun onHeadingUpdated(heading: Float)
}

/**
 * Location provider for testing without relying on simulator location spoofing.
 *
 * This allows for more granular unit tests.
 */
class SimulatedLocationProvider : LocationProvider {
    override var lastLocation: Location? = null
        set(value) {
            field = value
            onLocationUpdated()
        }
    override var lastHeading: Float? = null
        set(value) {
            field = value
            onHeadingUpdated()
        }

    private var listeners: List<LocationUpdateListener> = listOf()

    private var isUpdating = false
        set(value) {
            field = value

            onLocationUpdated()
            onHeadingUpdated()
        }

    override fun startUpdating() {
        isUpdating = true
    }

    override fun stopUpdating() {
        isUpdating = false
    }

    private fun onLocationUpdated() {
        val location = lastLocation
        if (isUpdating && location != null) {
            for (listener in listeners) {
                listener.onLocationUpdated(location)
            }
        }
    }

    private fun onHeadingUpdated() {
        val heading = lastHeading
        if (isUpdating && heading != null) {
            for (listener in listeners) {
                listener.onHeadingUpdated(heading)
            }
        }
    }
}