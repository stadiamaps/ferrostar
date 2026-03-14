package com.stadiamaps.ferrostar

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.maplibre.compose.camera.CameraState
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.extensions.incrementZoom
import com.maplibre.compose.camera.models.CameraPadding
import com.stadiamaps.ferrostar.core.DefaultNavigationViewModel
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.LocationProvider
import com.stadiamaps.ferrostar.core.LocationUpdateListener
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.core.annotation.AnnotationPublisher
import com.stadiamaps.ferrostar.core.annotation.valhalla.valhallaExtendedOSRMAnnotationPublisher
import com.stadiamaps.ferrostar.core.boundingBox
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLngBounds
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Heading
import uniffi.ferrostar.Route
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind

class DemoNavigationViewModel(
    // This is a simple example, but these would typically be dependency injected
    val ferrostarCore: FerrostarCore = AppModule.ferrostarCore,
    val locationProvider: LocationProvider = AppModule.locationProvider,
    val simulatedLocationProvider: SimulatedLocationProvider = AppModule.simulatedLocationProvider,
    annotationPublisher: AnnotationPublisher<*> = valhallaExtendedOSRMAnnotationPublisher()
) : DefaultNavigationViewModel(ferrostarCore, annotationPublisher), LocationUpdateListener {
  private val locationStateFlow = MutableStateFlow<UserLocation?>(null)
  val location = locationStateFlow.asStateFlow()
  private val executor = Executors.newSingleThreadScheduledExecutor()

  /** Whether navigation should use the simulated location provider. Settable at any time. */
  val isSimulating = mutableStateOf(true)

  private var lastRoute: Route? = null
  var lastDestinationName: String? = null
    private set

  // Here's an example of injecting a custom location into the navigation UI state when isNavigating
  // is false.
  override val navigationUiState: StateFlow<NavigationUiState> =
      combine(super.navigationUiState, locationStateFlow) { a, b -> Pair(a, b) }
          .map { (uiState, location) ->
            if (uiState.isNavigating()) {
              uiState
            } else {
              uiState.copy(location = location)
            }
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(),
              initialValue =
                  NavigationUiState(
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      false,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null))

  fun startLocationUpdates() {
    locationStateFlow.update { locationProvider.lastLocation }
    locationProvider.addListener(this, executor)
  }

  fun stopLocationUpdates() {
    locationProvider.removeListener(this)
  }

  override fun toggleMute() {
    val spokenInstructionObserver = ferrostarCore.spokenInstructionObserver
    if (spokenInstructionObserver == null) {
      Log.d("NavigationViewModel", "Spoken instruction observer is null, mute operation ignored.")
      return
    }
    spokenInstructionObserver.setMuted(!spokenInstructionObserver.isMuted)
  }

  override fun stopNavigation(stopLocationUpdates: Boolean) {
    ferrostarCore.stopNavigation(stopLocationUpdates = stopLocationUpdates)
  }

  /**
   * Fetches routes to [destination] and starts navigation on the first result.
   *
   * Suspends until a location fix is available, so it is safe to call immediately after launch.
   * Respects [isSimulating]: if true, switches the core to [simulatedLocationProvider] and starts
   * route playback.
   */
  fun startNavigation(destination: GeographicCoordinate, displayName: String? = null) {
    lastDestinationName = displayName ?: "%.4f, %.4f".format(destination.lat, destination.lng)
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val loc =
            if (isSimulating.value) {
              simulatedLocationProvider.lastLocation ?: location.first { it != null }
            } else {
              location.first { it != null }
            } ?: return@launch
        val routes =
            ferrostarCore.getRoutes(
                loc,
                listOf(Waypoint(coordinate = destination, kind = WaypointKind.BREAK)))
        val route = routes.first()
        lastRoute = route
        launchNavigation(route)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to start navigation", e)
      }
    }
  }

  /**
   * Enables simulated navigation. If navigation is already active, restarts it from the beginning
   * of the current route using the simulated provider (e.g. for DHU auto-drive testing).
   */
  fun enableAutoDriveSimulation() {
    isSimulating.value = true
    lastRoute?.let { route ->
      viewModelScope.launch(Dispatchers.IO) {
        try {
          launchNavigation(route)
        } catch (e: Exception) {
          Log.e(TAG, "Failed to enable auto-drive simulation", e)
        }
      }
    }
  }

  private fun launchNavigation(route: Route) {
    val activeProvider = if (isSimulating.value) simulatedLocationProvider else locationProvider
    ferrostarCore.setLocationProvider(activeProvider)
    ferrostarCore.startNavigation(route = route)
    if (isSimulating.value) {
      simulatedLocationProvider.setSimulatedRoute(route)
    }
  }

  override fun onLocationUpdated(location: UserLocation) {
    locationStateFlow.update { location }
  }

  override fun onHeadingUpdated(heading: Heading) {
    // TODO: Heading
  }

  // MapViewCamera
  // TODO: this is currently only used on the Android Auto screen. It will need updates to become shared.

  val mapViewCamera = mutableStateOf(
      MapViewCamera.TrackingUserLocation()
  )
  val cameraPadding = mutableStateOf(CameraPadding())
  val navigationCamera = mutableStateOf(MapViewCamera.TrackingUserLocationWithBearing(zoom = 16.0, pitch = 45.0))

  fun isTrackingUser(): Boolean =
      when (mapViewCamera.value.state) {
        is CameraState.TrackingUserLocation,
        is CameraState.TrackingUserLocationWithBearing -> true
        else -> false
      }

  fun zoomIn() {
    mapViewCamera.value = mapViewCamera.value.incrementZoom(1.0)
  }

  fun zoomOut() {
    mapViewCamera.value = mapViewCamera.value.incrementZoom(-1.0)
  }

  fun centerCamera() {
    if (isTrackingUser()) {
      centerOnRoute()
    } else {
      centerOnUser()
    }
  }

  private fun centerOnRoute() {
    val boundingBox = navigationUiState.value.routeGeometry?.boundingBox()
    boundingBox?.let {
      val latLngBounds = LatLngBounds.from(
          boundingBox.north,
          boundingBox.east,
          boundingBox.south,
          boundingBox.west
      )
      mapViewCamera.value = MapViewCamera.BoundingBox(
          latLngBounds,
          pitch = 0.0,
          padding = cameraPadding.value
      )
    }
  }

  private fun centerOnUser() {
    mapViewCamera.value = navigationCamera.value
  }

  companion object {
    const val TAG = "DemoNavigationViewModel"
  }
}
