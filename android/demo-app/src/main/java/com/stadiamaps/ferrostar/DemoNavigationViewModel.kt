package com.stadiamaps.ferrostar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stadiamaps.ferrostar.core.LocationProvider
import com.stadiamaps.ferrostar.core.LocationUpdateListener
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import uniffi.ferrostar.Heading
import uniffi.ferrostar.UserLocation

// NOTE: We are aware that this is not a particularly great ViewModel.
// We are working on improving this. See the discussion on
// https://github.com/stadiamaps/ferrostar/pull/295.
class DemoNavigationViewModel : ViewModel(), NavigationViewModel {
  private val locationStateFlow = MutableStateFlow<UserLocation?>(null)
  private val executor = Executors.newSingleThreadScheduledExecutor()

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

  override val uiState =
      locationStateFlow
          .map { userLocation ->
            // TODO: Heading
            NavigationUiState(
                userLocation, null, null, null, null, null, null, false, null, null, null, null)
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(),
              // TODO: Heading
              initialValue =
                  NavigationUiState(
                      null, null, null, null, null, null, null, false, null, null, null, null))

  override fun toggleMute() {
    // Do nothing
  }

  override fun stopNavigation() {
    // Do nothing
  }
}
