package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.maplibreui.routeline.RouteOverlayBuilder
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationCameraMode
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationCameraOptions
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationMapState
import com.stadiamaps.ferrostar.maplibreui.runtime.defaultNavigationCameraMode
import com.stadiamaps.ferrostar.maplibreui.runtime.nativeStyleOrNull
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationCameraOptions
import com.stadiamaps.ferrostar.maplibreui.runtime.rememberFerrostarLocationState
import com.stadiamaps.ferrostar.maplibreui.runtime.rememberNavigationMapState
import com.stadiamaps.ferrostar.maplibreui.runtime.toMapLibreLocation
import kotlinx.coroutines.flow.collectLatest
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.location.LocationPuck
import org.maplibre.compose.location.LocationPuckColors
import org.maplibre.compose.location.LocationPuckSizes
import org.maplibre.compose.location.LocationTrackingEffect
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.android.maps.Style
import uniffi.ferrostar.GeographicCoordinate

/**
 * The base MapLibre map configured for navigation with a route line, location puck, gesture
 * callbacks, and Ferrostar-specific camera behavior for phone and tablet use.
 *
 * @param styleUrl The MapLibre style URL to use for the map.
 * @param navigationMapState The Ferrostar-owned map state used to control follow, overview, free
 *   camera, and zoom behavior.
 * @param uiState The navigation UI state.
 * @param mapOptions The official MapLibre Compose options for ornaments, gestures, and map
 *   behavior.
 * @param routeOverlayBuilder The route overlay builder to use for rendering the route line.
 * @param navigationCameraOptions The camera templates applied when following the user in browsing
 *   and navigation modes.
 * @param locationPuckStyle The style to use for the official MapLibre location puck.
 * @param onMapReadyCallback A callback that is invoked when the underlying map style is ready to be
 *   interacted with.
 * @param onMapClick Callback invoked for taps on the map with geographic coordinates and screen
 *   position.
 * @param onMapLongClick Callback invoked for long presses on the map with geographic coordinates
 *   and screen position.
 * @param content Any additional composable map symbol content to render.
 */
@Composable
fun NavigationMapView(
    styleUrl: String,
    navigationMapState: NavigationMapState = rememberNavigationMapState(),
    uiState: NavigationUiState,
    mapOptions: MapOptions,
    routeOverlayBuilder: RouteOverlayBuilder = RouteOverlayBuilder.Default(),
    navigationCameraOptions: NavigationCameraOptions = navigationCameraOptions(),
    locationPuckStyle: NavigationMapPuckStyle = NavigationMapPuckStyle.Default(),
    onMapReadyCallback: ((Style) -> Unit)? = null,
    onMapClick: NavigationMapClickHandler = { _, _ -> NavigationMapClickResult.Pass },
    onMapLongClick: NavigationMapClickHandler = { _, _ -> NavigationMapClickResult.Pass },
    content: @Composable @MaplibreComposable ((NavigationUiState) -> Unit)? = null,
) {
  val cameraState = navigationMapState.cameraState
  val userLocationState = rememberFerrostarLocationState(uiState.location)
  val userLocation = uiState.location?.toMapLibreLocation()
  navigationMapState.navigationCameraOptions = navigationCameraOptions

  var isNavigating by remember { mutableStateOf(uiState.isNavigating()) }
  var mapReadyCallbackFired by remember(styleUrl, onMapReadyCallback) { mutableStateOf(false) }
  if (uiState.isNavigating() != isNavigating) {
    isNavigating = uiState.isNavigating()
    navigationMapState.cameraMode = defaultNavigationCameraMode(isNavigating)
  }

  LocationTrackingEffect(
      locationState = userLocationState,
      enabled = navigationMapState.isTrackingUser,
      trackBearing = navigationMapState.cameraMode == NavigationCameraMode.FOLLOW_USER_WITH_BEARING,
  ) {
    cameraState.position =
        navigationMapState.followingCameraPosition(
            target = currentLocation.position,
            bearing = currentLocation.bearing,
        )
  }

  LaunchedEffect(cameraState, navigationMapState) {
    snapshotFlow { cameraState.moveReason }.collectLatest { moveReason ->
      if (moveReason == CameraMoveReason.GESTURE && navigationMapState.isTrackingUser) {
        navigationMapState.cameraMode = NavigationCameraMode.FREE
      }
    }
  }

  MaplibreMap(
      modifier = Modifier.fillMaxSize(),
      baseStyle = BaseStyle.Uri(styleUrl),
      cameraState = cameraState,
      onMapClick = { position, screenPosition ->
        onMapClick(position.toGeographicCoordinate(), screenPosition).toComposeClickResult()
      },
      onMapLongClick = { position, screenPosition ->
        onMapLongClick(position.toGeographicCoordinate(), screenPosition).toComposeClickResult()
      },
      onMapLoadFinished = {
        if (userLocation != null && navigationMapState.isTrackingUser) {
          cameraState.position =
              navigationMapState.followingCameraPosition(
                  target = userLocation.position,
                  bearing = userLocation.bearing,
              )
        }

        if (!mapReadyCallbackFired) {
          if (onMapReadyCallback != null) {
            cameraState.nativeStyleOrNull()?.let {
              mapReadyCallbackFired = true
              onMapReadyCallback(it)
            } ?: run {
              mapReadyCallbackFired = true
            }
          } else {
            mapReadyCallbackFired = true
          }
        }
      },
      options = mapOptions,
  ) {
    routeOverlayBuilder.navigationPath(uiState)

    LocationPuck(
        idPrefix = "ferrostar-location",
        locationState = userLocationState,
        cameraState = cameraState,
        colors =
            LocationPuckColors(
                dotFillColorCurrentLocation = locationPuckStyle.dotFillColorCurrentLocation,
                dotFillColorOldLocation = locationPuckStyle.dotFillColorOldLocation,
                dotStrokeColor = locationPuckStyle.dotStrokeColor,
                shadowColor = locationPuckStyle.shadowColor,
                accuracyStrokeColor = locationPuckStyle.accuracyStrokeColor,
                accuracyFillColor = locationPuckStyle.accuracyFillColor,
                bearingColor = locationPuckStyle.bearingColor,
            ),
        sizes =
            LocationPuckSizes(
                dotRadius = locationPuckStyle.dotRadius,
                dotStrokeWidth = locationPuckStyle.dotStrokeWidth,
            ),
        showBearing = locationPuckStyle.showBearing,
        showBearingAccuracy = locationPuckStyle.showBearingAccuracy,
    )

    if (content != null) {
      content(uiState)
    }
  }
}

private fun org.maplibre.spatialk.geojson.Position.toGeographicCoordinate(): GeographicCoordinate =
    GeographicCoordinate(lat = latitude, lng = longitude)

private fun NavigationMapClickResult.toComposeClickResult(): ClickResult =
    when (this) {
      NavigationMapClickResult.Pass -> ClickResult.Pass
      NavigationMapClickResult.Consume -> ClickResult.Consume
    }

private fun NavigationMapState.followingCameraPosition(
    target: org.maplibre.spatialk.geojson.Position,
    bearing: Double?,
): org.maplibre.compose.camera.CameraPosition =
    when (cameraMode) {
      NavigationCameraMode.FOLLOW_USER -> navigationCameraOptions.browsingUser(target)
      NavigationCameraMode.FOLLOW_USER_WITH_BEARING ->
          navigationCameraOptions.navigatingUser(
              target = target,
              bearing = bearing ?: cameraState.position.bearing,
          )
      else -> cameraState.position
    }
