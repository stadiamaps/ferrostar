package com.stadiamaps.ferrostar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.LocationProvider
import com.stadiamaps.ferrostar.core.LocationUpdateListener
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.extensions.progress
import com.stadiamaps.ferrostar.core.isNavigating
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import uniffi.ferrostar.Heading
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation

// NOTE: We are aware that this is not a particularly great ViewModel.
// We are working on improving this. See the discussion on
// https://github.com/stadiamaps/ferrostar/pull/295.
class DemoNavigationViewModel(
    // This is a simple example, but these would typically be dependency injected
    private val ferrostarCore: FerrostarCore = AppModule.ferrostarCore,
) : ViewModel(), NavigationViewModel {
  private val locationStateFlow = MutableStateFlow<UserLocation?>(null)
  private val executor = Executors.newSingleThreadScheduledExecutor()

  private val muteState: StateFlow<Boolean?> =
      ferrostarCore.spokenInstructionObserver?.muteState ?: MutableStateFlow(null)

  fun startLocationUpdates(locationProvider: LocationProvider) {
    locationStateFlow.update { locationProvider.lastLocation }
    locationProvider.addListener(
        object : LocationUpdateListener {
          override fun onLocationUpdated(location: UserLocation) {
            locationStateFlow.update { location }
          }

          override fun onHeadingUpdated(heading: Heading) {
            // TODO: Heading
          }
        },
        executor)
  }

  override val uiState: StateFlow<NavigationUiState> =
      combine(ferrostarCore.state, muteState, locationStateFlow) { a, b, c -> Triple(a, b, c) }
          .map { (ferrostarCoreState, isMuted, userLocation) ->
            if (ferrostarCoreState.isNavigating()) {
              val tripState = ferrostarCoreState.tripState
              val location = ferrostarCore.locationProvider.lastLocation
              val snappedLocation =
                  when (tripState) {
                    is TripState.Navigating -> tripState.snappedUserLocation
                    is TripState.Complete,
                    TripState.Idle -> ferrostarCore.locationProvider.lastLocation
                  }
              NavigationUiState.fromFerrostar(
                  ferrostarCoreState, isMuted, location, snappedLocation)
            } else {
              // TODO: Heading
              NavigationUiState(
                  userLocation, null, null, null, null, null, null, false, null, null, null, null)
            }
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(),
              // TODO: Heading
              initialValue =
                  NavigationUiState(
                      null, null, null, null, null, null, null, false, null, null, null, null))

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
}
