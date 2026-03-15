package com.stadiamaps.ferrostar.core.location

import android.location.Location
import android.os.Build
import android.os.SystemClock
import java.time.Instant
import uniffi.ferrostar.CourseOverGround
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Speed
import uniffi.ferrostar.UserLocation

fun Location.toUserLocation(): UserLocation {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    UserLocation(
        GeographicCoordinate(latitude, longitude),
        if (hasAccuracy()) {
          accuracy.toDouble()
        } else {
          Double.MAX_VALUE
        },
        if (hasBearing()) {
          CourseOverGround(
              bearing.toUInt().toUShort(),
              if (hasBearingAccuracy()) {
                bearingAccuracyDegrees.toUInt().toUShort()
              } else {
                null
              })
        } else {
          null
        },
        Instant.ofEpochMilli(time),
        if (hasSpeed() && hasSpeedAccuracy()) {
          Speed(speed.toDouble(), speedAccuracyMetersPerSecond.toDouble())
        } else {
          null
        })
  } else {
    UserLocation(
        GeographicCoordinate(latitude, longitude),
        if (hasAccuracy()) {
          accuracy.toDouble()
        } else {
          Double.MAX_VALUE
        },
        if (hasBearing()) {
          CourseOverGround(bearing.toUInt().toUShort(), null)
        } else {
          null
        },
        Instant.ofEpochMilli(time),
        if (hasSpeed()) {
          Speed(speed.toDouble(), null)
        } else {
          null
        })
  }
}

fun UserLocation.toAndroidLocation(): Location {
  val location = Location("FerrostarCore")

  location.latitude = this.coordinates.lat
  location.longitude = this.coordinates.lng
  location.accuracy = this.horizontalAccuracy.toFloat()

  // NOTE: We have a lot of checks in place which we could remove (+ improve correctness)
  // if we supported API 26.
  val course = this.courseOverGround
  if (course != null) {
    location.bearing = course.degrees.toFloat()

    val accuracy = course.accuracy
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && accuracy != null) {
      // NOTE: Course accuracy information is not available until API 26
      location.bearingAccuracyDegrees = accuracy.toFloat()
    }
  }

  location.time = this.timestamp.toEpochMilli()

  // FIXME: This is not entirely correct, but might be an acceptable approximation.
  // Feedback welcome as the purpose is not really documented.
  location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

  return location
}
