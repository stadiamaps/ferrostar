package com.stadiamaps.ferrostar.core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uniffi.ferrostar.Disposable
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavigationControllerInterface
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import java.util.concurrent.Executors

data class NavigationUiState(
    val snappedLocation: UserLocation,
    val heading: Float?,
    val routeGeometry: List<GeographicCoordinate>
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
    initialUserLocation: Location,
    routeGeometry: List<GeographicCoordinate>,
) : ViewModel(), LocationUpdateListener {
    private val _executor = Executors.newSingleThreadExecutor()
    private var _state = navigationController.getInitialState(initialUserLocation.userLocation())

    private val _uiState = MutableStateFlow(
        NavigationUiState(
            snappedLocation = initialUserLocation.userLocation(),
            // TODO: Heading/course over ground
            heading = null,
            routeGeometry = routeGeometry
        )
    )
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    init {
        locationProvider.addListener(this, _executor)
    }

    private fun update(newState: TripState, location: Location) {
        _state = newState
        _uiState.update { currentValue ->
            currentValue.copy(
                snappedLocation = location.userLocation(),
            )
        }
    }

    override fun onLocationUpdated(location: Location) {
        update(
            newState = navigationController.updateUserLocation(
                location = location.userLocation(),
                state = _state
            ), location = location
        )
    }

    override fun onHeadingUpdated(heading: Float) {
        _uiState.update { currentValue ->
            currentValue.copy(
                heading = heading
            )
        }
    }

    override fun onCleared() {
        locationProvider.removeListener(this)
        _executor.shutdown()

        if (navigationController is Disposable) {
            navigationController.destroy()
        }
    }
}
