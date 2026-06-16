package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.maplibre.compose.location.BearingWithAccuracy
import org.maplibre.compose.location.Location
import org.maplibre.compose.location.LocationProvider
import org.maplibre.compose.location.PositionWithAccuracy
import org.maplibre.compose.location.SpeedWithAccuracy
import org.maplibre.compose.location.UserLocationState
import org.maplibre.compose.location.rememberUserLocationState
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.degrees
import org.maplibre.spatialk.units.extensions.inDegrees
import org.maplibre.spatialk.units.extensions.meters
import uniffi.ferrostar.UserLocation

internal fun UserLocation.toMapLibreLocation(): Location =
    Location(
        position =
            PositionWithAccuracy(
                value = Position(coordinates.lng, coordinates.lat),
                accuracy = horizontalAccuracy.meters,
            ),
        course =
            courseOverGround?.let { course ->
              BearingWithAccuracy(
                  value = Bearing.North + course.degrees.toDouble().degrees,
                  accuracy = course.accuracy?.toDouble()?.degrees,
              )
            },
        speed =
            speed?.let { speed ->
              SpeedWithAccuracy(
                  distancePerSecond = speed.value.meters,
                  accuracy = speed.accuracy?.meters,
              )
            },
        timestamp = TimeSource.Monotonic.markNow(),
    )

internal val Location.courseDegrees: Double?
  get() = course?.value?.let { (it - Bearing.North).inDegrees }

private class FerrostarLocationProvider : LocationProvider {
  private val locationState = MutableStateFlow<Location?>(null)

  override val location: StateFlow<Location?> = locationState

  fun update(location: Location?) {
    locationState.value = location
  }
}

@Composable
internal fun rememberFerrostarLocationState(userLocation: UserLocation?): UserLocationState {
  val provider = remember { FerrostarLocationProvider() }

  LaunchedEffect(userLocation) { provider.update(userLocation?.toMapLibreLocation()) }

  return rememberUserLocationState(provider)
}
