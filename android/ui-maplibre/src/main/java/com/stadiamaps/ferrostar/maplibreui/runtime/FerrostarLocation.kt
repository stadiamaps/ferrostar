package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.TimeSource
import org.maplibre.compose.location.Location
import org.maplibre.compose.location.LocationProvider
import org.maplibre.compose.location.UserLocationState
import org.maplibre.compose.location.rememberUserLocationState
import org.maplibre.spatialk.geojson.Position
import uniffi.ferrostar.UserLocation

internal fun UserLocation.toMapLibreLocation(): Location =
    Location(
        position = Position(coordinates.lng, coordinates.lat),
        accuracy = horizontalAccuracy,
        bearing = courseOverGround?.degrees?.toDouble(),
        bearingAccuracy = courseOverGround?.accuracy?.toDouble(),
        speed = speed?.value,
        speedAccuracy = speed?.accuracy,
        timestamp = TimeSource.Monotonic.markNow(),
    )

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

  LaunchedEffect(userLocation) {
    provider.update(userLocation?.toMapLibreLocation())
  }

  return rememberUserLocationState(provider)
}
