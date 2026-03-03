package com.stadiamaps.ferrostar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stadiamaps.autocomplete.AutocompleteSearch
import com.stadiamaps.autocomplete.center
import com.stadiamaps.ferrostar.composeui.views.components.gridviews.InnerGridView
import com.stadiamaps.ferrostar.core.LocationProvider
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.core.toAndroidLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteOverlay(
    modifier: Modifier = Modifier,
    scope: CoroutineScope,
    isNavigating: Boolean,
    locationProvider: LocationProvider,
    loc: UserLocation
) {
  if (!isNavigating) {
    InnerGridView(
        modifier = modifier.fillMaxSize().padding(bottom = 16.dp, top = 16.dp),
        topCenter = {
          AppModule.stadiaApiKey?.let { apiKey ->
            AutocompleteSearch(apiKey = apiKey, userLocation = loc.toAndroidLocation()) { feature ->
              feature.center()?.let { center ->
                // Fetch a route in the background
                scope.launch(Dispatchers.IO) {
                  // TODO: Fail gracefully
                  val routes =
                      AppModule.ferrostarCore.getRoutes(
                          loc,
                          listOf(
                              Waypoint(
                                  coordinate =
                                      GeographicCoordinate(center.latitude, center.longitude),
                                  kind = WaypointKind.BREAK),
                          ))

                  val route = routes.first()
                  AppModule.ferrostarCore.startNavigation(route = route)

                  if (locationProvider is SimulatedLocationProvider) {
                    locationProvider.setSimulatedRoute(route)
                  }
                }
              }
            }
          }
        })
  }
}
