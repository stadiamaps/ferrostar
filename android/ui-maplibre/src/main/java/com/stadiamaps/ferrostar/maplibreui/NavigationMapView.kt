package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.maplibreui.routeline.RouteOverlayBuilder
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationCameraMode
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationCameraOptions
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationMapState
import com.stadiamaps.ferrostar.maplibreui.runtime.TrackingCameraEffect
import com.stadiamaps.ferrostar.maplibreui.runtime.courseDegrees
import com.stadiamaps.ferrostar.maplibreui.runtime.defaultNavigationCameraMode
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationCameraOptions
import com.stadiamaps.ferrostar.maplibreui.runtime.rememberDisplayedNavigationLocation
import com.stadiamaps.ferrostar.maplibreui.runtime.rememberNavigationMapState
import com.stadiamaps.ferrostar.maplibreui.runtime.snapTrackingCameraToUserLocation
import com.stadiamaps.ferrostar.maplibreui.runtime.toLocationMeasurement
import kotlinx.coroutines.flow.collectLatest
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.interaction.ClickEvent
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.LocalViewportInsets
import org.maplibre.compose.overlay.include
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.MaplibreComposable
import uniffi.ferrostar.GeographicCoordinate

/**
 * The base MapLibre map configured for navigation with a route line, location puck, gesture
 * callbacks, and Ferrostar-specific camera behavior for phone and tablet use.
 *
 * @param baseStyle The MapLibre base style to use for the map.
 * @param navigationMapState The Ferrostar-owned map state used to control follow, overview, free
 *   camera, and zoom behavior. This view attaches its MapLibre map state to it.
 * @param uiState The navigation UI state.
 * @param mapOptions The MapLibre settings for rendering, input, and the controls drawn over the
 *   map.
 * @param routeOverlayBuilder The route overlay builder to use for rendering the route line.
 * @param navigationCameraOptions The camera templates applied when following the user in browsing
 *   and navigation modes.
 * @param locationPuckStyle The style to use for the location indicator.
 * @param showDefaultPuck Whether Ferrostar should render its built-in location puck.
 * @param onMapLoadFinished Runs when the base style finishes loading.
 * @param onMapLoadFailed Runs with the failure reason when the base style fails to load.
 * @param onMapClick Callback invoked for taps on the map that no layer consumes, with geographic
 *   coordinates and screen position.
 * @param onMapLongClick Callback invoked for long presses on the map with geographic coordinates
 *   and screen position. It runs before the long click handlers of layers.
 * @param content Any additional composable map symbol content to render.
 */
@Composable
@UiComposable
fun NavigationMapView(
    baseStyle: BaseStyle,
    navigationMapState: NavigationMapState = rememberNavigationMapState(),
    uiState: NavigationUiState,
    mapOptions: NavigationMapOptions = NavigationMapOptions(),
    routeOverlayBuilder: RouteOverlayBuilder? = RouteOverlayBuilder.Default(),
    navigationCameraOptions: NavigationCameraOptions = navigationCameraOptions(),
    locationPuckStyle: NavigationMapPuckStyle = NavigationMapPuckStyle(),
    showDefaultPuck: Boolean = true,
    onMapLoadFinished: () -> Unit = {},
    onMapLoadFailed: (String?) -> Unit = {},
    onMapClick: NavigationMapClickHandler = { _, _ -> NavigationMapClickResult.Pass },
    onMapLongClick: NavigationMapClickHandler = { _, _ -> NavigationMapClickResult.Pass },
    content: @Composable @MaplibreComposable ((NavigationUiState) -> Unit)? = null,
) {
  val displayedNavigationLocation = rememberDisplayedNavigationLocation(uiState)
  var lastKnownNavigationPuckBearing by remember { mutableStateOf(0.0) }
  navigationMapState.navigationCameraOptions = navigationCameraOptions

  var isNavigating by remember { mutableStateOf(uiState.isNavigating()) }
  if (uiState.isNavigating() != isNavigating) {
    isNavigating = uiState.isNavigating()
    navigationMapState.cameraMode = defaultNavigationCameraMode(isNavigating)
  }

  LaunchedEffect(displayedNavigationLocation?.courseDegrees) {
    displayedNavigationLocation?.courseDegrees?.let { lastKnownNavigationPuckBearing = it }
  }

  val mapState =
      rememberMapState(
          baseStyle = baseStyle,
          // Only a new map reads this. Observing it would recompose this view on every camera
          // frame.
          initialCameraPosition =
              Snapshot.withoutReadObservation { navigationMapState.cameraPosition },
      ) {
        routeOverlayBuilder?.navigationPath(uiState)

        if (showDefaultPuck) {
          if (shouldRenderNavigationPuck(uiState) && displayedNavigationLocation != null) {
            NavigationPuckOverlay(
                target =
                    NavigationPuckTarget(
                        longitude = displayedNavigationLocation.position.longitude,
                        latitude = displayedNavigationLocation.position.latitude,
                        bearingDegrees =
                            navigationPuckBearingDegrees(
                                currentBearing = displayedNavigationLocation.courseDegrees,
                                lastKnownBearing = lastKnownNavigationPuckBearing,
                            ),
                    ),
                style = locationPuckStyle,
            )
          } else {
            UserLocationIndicator(
                location = uiState.location?.toLocationMeasurement(),
                style = locationPuckStyle,
            )
          }
        }

        if (content != null) {
          content(uiState)
        }
      }
  // Attach during composition so that the tracking camera below can move this map on this frame.
  navigationMapState.attach(mapState)
  DisposableEffect(navigationMapState, mapState) {
    onDispose { navigationMapState.detach(mapState) }
  }

  TrackingCameraEffect(
      navigationMapState = navigationMapState,
      userLocation = displayedNavigationLocation,
  )

  LaunchedEffect(mapState, navigationMapState) {
    snapshotFlow { mapState.cameraMoveReason }
        .collectLatest { moveReason ->
          if (moveReason == CameraMoveReason.Gesture && navigationMapState.isTrackingUser) {
            navigationMapState.cameraMode = NavigationCameraMode.FREE
          }
        }
  }

  val currentDisplayedNavigationLocation by rememberUpdatedState(displayedNavigationLocation)
  val currentOnMapLoadFinished by rememberUpdatedState(onMapLoadFinished)
  val currentOnMapLoadFailed by rememberUpdatedState(onMapLoadFailed)
  LaunchedEffect(mapState, navigationMapState) {
    mapState.events.collect { event ->
      when (event) {
        MapEvent.StyleLoaded -> {
          val location = currentDisplayedNavigationLocation
          if (location != null && navigationMapState.isTrackingUser) {
            navigationMapState.snapTrackingCameraToUserLocation(location)
          }
          currentOnMapLoadFinished()
        }
        is MapEvent.StyleLoadFailed -> currentOnMapLoadFailed(event.reason)
        else -> Unit
      }
    }
  }

  val currentOnMapClick by rememberUpdatedState(onMapClick)
  val currentOnMapLongClick by rememberUpdatedState(onMapLongClick)
  val interactions =
      remember(mapOptions.interactions) {
        MapInteractions(from = mapOptions.interactions) {
          callbacks {
            click { onUnhandled { event -> event.dispatchTo(currentOnMapClick) } }
            longClick { onEvent { event -> event.dispatchTo(currentOnMapLongClick) } }
          }
        }
      }

  MaplibreMap(
      modifier = Modifier.fillMaxSize(),
      state = mapState,
      cameraConstraints = mapOptions.cameraConstraints,
      renderOptions = mapOptions.renderOptions,
      interactions = interactions,
      uiOptions = mapOptions.uiOptions,
  ) {
    CompositionLocalProvider(LocalViewportInsets provides mapOptions.overlayPadding) {
      include(mapOptions.overlay)
    }
  }
}

private fun ClickEvent.dispatchTo(handler: NavigationMapClickHandler): ClickResult {
  val position = position ?: return ClickResult.Pass
  val coordinate =
      GeographicCoordinate(lat = position.latitude, lng = wrapLongitude(position.longitude))
  return when (handler(coordinate, screenOffset)) {
    NavigationMapClickResult.Pass -> ClickResult.Pass
    NavigationMapClickResult.Consume -> ClickResult.Consume
  }
}

/** Wraps a longitude from any world copy into `[-180, 180)`. */
internal fun wrapLongitude(longitude: Double): Double =
    ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
