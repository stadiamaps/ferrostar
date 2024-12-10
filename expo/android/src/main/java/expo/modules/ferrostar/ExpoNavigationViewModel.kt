package expo.modules.ferrostar

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.stadiamaps.ferrostar.core.DefaultNavigationViewModel
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.LocationProvider
import com.stadiamaps.ferrostar.core.LocationUpdateListener
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.annotation.AnnotationPublisher
import com.stadiamaps.ferrostar.core.annotation.valhalla.valhallaExtendedOSRMAnnotationPublisher
import com.stadiamaps.ferrostar.core.isNavigating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Heading
import uniffi.ferrostar.Route
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import java.time.Instant
import java.util.concurrent.Executors

class ExpoNavigationViewModel(private val ferrostarCore: FerrostarCore, annotationPublisher: AnnotationPublisher<*> = valhallaExtendedOSRMAnnotationPublisher()) : DefaultNavigationViewModel(ferrostarCore, annotationPublisher), LocationUpdateListener {
    private val locationStateFlow = MutableStateFlow(UserLocation(
        GeographicCoordinate(-43.525650, 172.639847),
        0.0,
        null,
        Instant.now(),
        null
    ))
    private val executor = Executors.newSingleThreadScheduledExecutor()

    private val muteState: StateFlow<Boolean?> =
        ferrostarCore.spokenInstructionObserver?.muteState ?: MutableStateFlow(null)

    fun startLocationUpdates(locationProvider: LocationProvider) {
        if (locationProvider.lastLocation != null) {
            locationStateFlow.update { locationProvider.lastLocation!! }
        }
        locationProvider.addListener(this, executor)
    }

    fun stopLocationUpdates(locationProvider: LocationProvider) {
        locationProvider.removeListener(this)
    }

    val previewRoute: MutableStateFlow<Route?> = MutableStateFlow(null)

    override val navigationUiState: StateFlow<NavigationUiState> =
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

    override fun onLocationUpdated(location: UserLocation) {
        locationStateFlow.update { location }
    }

    override fun onHeadingUpdated(heading: Heading) {
        // TODO: Heading
    }
}