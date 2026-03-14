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
import com.stadiamaps.ferrostar.core.toAndroidLocation
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.UserLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteOverlay(
    modifier: Modifier = Modifier,
    viewModel: DemoNavigationViewModel,
    isNavigating: Boolean,
    loc: UserLocation
) {
  if (!isNavigating) {
    InnerGridView(
        modifier = modifier.fillMaxSize().padding(bottom = 16.dp, top = 16.dp),
        topCenter = {
          AppModule.stadiaApiKey?.let { apiKey ->
            AutocompleteSearch(apiKey = apiKey, userLocation = loc.toAndroidLocation()) { feature ->
              feature.center()?.let { center ->
                viewModel.startNavigation(GeographicCoordinate(center.latitude, center.longitude))
              }
            }
          }
        })
  }
}
