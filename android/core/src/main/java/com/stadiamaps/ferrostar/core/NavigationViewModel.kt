package com.stadiamaps.ferrostar.core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import uniffi.ferrostar.Disposable
import uniffi.ferrostar.NavigationControllerInterface
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import java.util.concurrent.Executors

data class NavigationUiState(
    val snappedLocation: Location,
    val heading: Float?
)

/**
 * A view model for integrating state into an Android application.
 *
 * Uses [androidx.lifecycle.ViewModel].
 * Note that it is assumed that the passed in [navigationController]
 * either requires no finalization OR that it conforms to [Disposable].
 * In the case that it conforms to [Disposable],
 * the [navigationController] will be automatically destroyed in [onCleared].
 */
class NavigationViewModel(
    private val navigationController: NavigationControllerInterface,
    private val locationProvider: LocationProvider,
    initialUserLocation: UserLocation,
) : ViewModel(), LocationUpdateListener {
    // TODO: Is this the best executor?
    private val _executor = Executors.newSingleThreadExecutor()
    private var _state = navigationController.getInitialState(initialUserLocation)
    // TODO: UI state flow?
//    private val _uiState = MutableStateFlow(NavigationUiState(snappedLocation = navigationController.))

    init {
        locationProvider.addListener(this, _executor)
    }

    override fun onLocationUpdated(location: Location) {
        _state = navigationController.updateUserLocation(location = location.userLocation(), state = _state)
        // TODO: Update view model
    }

    override fun onHeadingUpdated(heading: Float) {
        // TODO: Update view model
        TODO("Not yet implemented")
    }

    override fun onCleared() {
        locationProvider.removeListener(this)
        _executor.shutdown()

        if (navigationController is Disposable) {
            navigationController.destroy()
        }
    }
}
