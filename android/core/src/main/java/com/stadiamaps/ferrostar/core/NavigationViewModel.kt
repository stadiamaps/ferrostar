package com.stadiamaps.ferrostar.core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uniffi.ferrostar.Disposable
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavigationControllerInterface
import uniffi.ferrostar.SpokenInstruction
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.VisualInstruction
import java.util.concurrent.Executors

data class NavigationUiState(
    val snappedLocation: UserLocation,
    val heading: Float?,
    val routeGeometry: List<GeographicCoordinate>,
    val visualInstruction: VisualInstruction?,
    val spokenInstruction: SpokenInstruction?,
    val distanceToNextManeuver: Double?,
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
            routeGeometry = routeGeometry,
            visualInstruction = visualInstructionForState(_state),
            spokenInstruction = null,
            distanceToNextManeuver = distanceForState(_state)
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
                // TODO: Heading/course over ground
                visualInstruction = visualInstructionForState(newState),
                distanceToNextManeuver = distanceForState(newState)
            )
        }
    }

    private fun distanceForState(newState: TripState) = when (newState) {
        is TripState.Navigating -> newState.distanceToNextManeuver
        is TripState.Complete -> null
    }

    private fun visualInstructionForState(newState: TripState) = try {
        when (newState) {
            // TODO: This isn't great; the core should probably just tell us which instruction to display
            is TripState.Navigating -> newState.remainingSteps.first().visualInstructions.last {
                newState.distanceToNextManeuver <= it.triggerDistanceBeforeManeuver
            }

            is TripState.Complete -> null
        }
    } catch (_: NoSuchElementException) {
        null
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
