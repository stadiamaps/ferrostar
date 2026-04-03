package com.stadiamaps.ferrostar

import android.location.Location
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.stadiamaps.ferrostar.core.DefaultNavigationViewModel
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.annotation.AnnotationPublisher
import com.stadiamaps.ferrostar.core.annotation.valhalla.valhallaExtendedOSRMAnnotationPublisher
import com.stadiamaps.ferrostar.core.location.NavigationLocationProvider
import com.stadiamaps.ferrostar.core.location.toUserLocation
import com.stadiamaps.ferrostar.support.initialSimulatedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind

data class DestinationSelection(
    val coordinate: GeographicCoordinate,
    val label: String? = null,
    val origin: DestinationSelectionOrigin = DestinationSelectionOrigin.MapLongPress,
)

enum class DestinationSelectionOrigin {
  MapLongPress,
  SearchResult,
}

data class DemoNavigationSceneState(
    val droppedPin: GeographicCoordinate? = null,
    val selectedDestination: DestinationSelection? = null,
    val isDestinationSheetVisible: Boolean = false,
    val destinationSheetHeightPx: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DemoNavigationViewModel(
    // This is a simple example, but these would typically be dependency injected
    val ferrostarCore: FerrostarCore = AppModule.ferrostarCore,
    val locationProvider: NavigationLocationProvider = AppModule.locationProvider,
    annotationPublisher: AnnotationPublisher<*> = valhallaExtendedOSRMAnnotationPublisher()
) : DefaultNavigationViewModel(ferrostarCore, annotationPublisher) {

  private val _hasLocationPermission = MutableStateFlow(false)

  private val _simulated = MutableStateFlow(false)
  val simulated = _simulated.asStateFlow()

  private val locationStateFlow = MutableStateFlow<UserLocation?>(null)
  val location = locationStateFlow.asStateFlow()

  private val _sceneState = MutableStateFlow(DemoNavigationSceneState())
  val sceneState = _sceneState.asStateFlow()

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
              initialValue = NavigationUiState.empty()
          )

  init {
    viewModelScope.launch {
      _hasLocationPermission
          .flatMapLatest { hasPermission ->
            if (!hasPermission) {
              flowOf(initialSimulatedLocation)
            } else {
              locationProvider.locationUpdates(5000L)
                  .map { it.toUserLocation() }
            }
          }
          .collect {
            locationStateFlow.emit(it)
          }
    }
  }

  fun setLocationPermissions(permitted: Boolean) {
    _hasLocationPermission.value = permitted
  }

  fun toggleSimulation() {
    _simulated.value = !_simulated.value
    if (!_simulated.value) {
      locationProvider.disableSimulation()
    }
  }

  fun enableAutoDriveSimulation() {
    _simulated.value = true
  }

  fun selectDestination(
      coordinate: GeographicCoordinate,
      label: String? = null,
      origin: DestinationSelectionOrigin = DestinationSelectionOrigin.MapLongPress,
  ) {
    _sceneState.value =
        _sceneState.value.copy(
            droppedPin = coordinate,
            selectedDestination =
                DestinationSelection(coordinate = coordinate, label = label, origin = origin),
            isDestinationSheetVisible = true,
        )
  }

  fun selectDestination(
      location: Location,
      label: String? = null,
      origin: DestinationSelectionOrigin = DestinationSelectionOrigin.MapLongPress,
  ) {
    selectDestination(
        coordinate = GeographicCoordinate(location.latitude, location.longitude),
        label = label,
        origin = origin,
    )
  }

  fun clearSelectedDestination() {
    _sceneState.value =
        _sceneState.value.copy(
            droppedPin = null,
            selectedDestination = null,
            isDestinationSheetVisible = false,
            destinationSheetHeightPx = 0,
        )
  }

  fun hideDestinationSheet() {
    _sceneState.value =
        _sceneState.value.copy(
            isDestinationSheetVisible = false,
            destinationSheetHeightPx = 0,
        )
  }

  fun setDestinationSheetHeight(heightPx: Int) {
    if (_sceneState.value.destinationSheetHeightPx == heightPx) {
      return
    }
    _sceneState.value = _sceneState.value.copy(destinationSheetHeightPx = heightPx)
  }

  fun startSelectedDestinationNavigation() {
    val destination = sceneState.value.selectedDestination ?: return
    clearSelectedDestination()
    startNavigation(destination.coordinate, destination.label)
  }

  override fun toggleMute() {
    val spokenInstructionObserver = ferrostarCore.spokenInstructionObserver
    if (spokenInstructionObserver == null) {
      Log.d("NavigationViewModel", "Spoken instruction observer is null, mute operation ignored.")
      return
    }
    spokenInstructionObserver.setMuted(!spokenInstructionObserver.isMuted)
  }

  fun startNavigation(destination: Location, name: String?) {
    startNavigation(
        destination = GeographicCoordinate(destination.latitude, destination.longitude),
        name = name,
    )
  }

  fun startNavigation(destination: GeographicCoordinate, name: String? = null) {
    viewModelScope.launch(Dispatchers.IO) {
      // TODO: Fail gracefully
      val lastLocation = location.value ?: return@launch

      // TODO: Add label to waypoint?
      // TODO: Assign the destination to the `NavigationManagerBridge`
      Log.d(TAG, "fetching route to $destination with name $name")
      val routes =
          ferrostarCore.getRoutes(
              lastLocation,
              listOf(
                  Waypoint(
                      coordinate = destination,
                      kind = WaypointKind.BREAK),
              ))

      val route = routes.first()

      if (simulated.value) {
        locationProvider.enableSimulationOn(route)
      }

      if (navigationUiState.value.isNavigating()) {
        ferrostarCore.replaceRoute(route = route)
      } else {
        ferrostarCore.startNavigation(route = route)
      }
    }
  }

  override fun stopNavigation() {
    locationProvider.disableSimulation()
    ferrostarCore.stopNavigation()
  }

  companion object {
    const val TAG = "DemoNavigationViewModel"
  }
}
