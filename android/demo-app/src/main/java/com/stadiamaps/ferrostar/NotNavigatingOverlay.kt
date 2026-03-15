package com.stadiamaps.ferrostar

import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import com.stadiamaps.autocomplete.AutocompleteSearch
import com.stadiamaps.autocomplete.center
import com.stadiamaps.ferrostar.composeui.views.components.gridviews.InnerGridView
import com.stadiamaps.ferrostar.core.location.toAndroidLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotNavigatingOverlay(
    modifier: Modifier = Modifier,
    viewModel: DemoNavigationViewModel,
) {
  val location by viewModel.location.collectAsState()
  val isSimulating by viewModel.simulated.collectAsState()
  val uiState by viewModel.navigationUiState.collectAsState()

  if (!uiState.isNavigating()) {
    InnerGridView(
        modifier = modifier.fillMaxSize().padding(bottom = 16.dp, top = 16.dp),
        topCenter = {
          AppModule.stadiaApiKey?.let { apiKey ->
            AutocompleteSearch(
                apiKey = apiKey,
                userLocation = location?.toAndroidLocation()
            ) { feature ->
              feature.center()?.let { center ->
                viewModel.startNavigation(center)
              }
            }
          }
        },
        bottomEnd = {
          Column(
              modifier = Modifier.padding(bottom = 24.dp),
              horizontalAlignment = Alignment.End
          ) {
            Button({ viewModel.toggleSimulation() }) {
              val nextLocation = if (!isSimulating) {
                "simulated"
              } else {
                "GPS"
              }
              Text("Set location to $nextLocation")
            }

            val currentLocation = if (isSimulating) {
              "simulated"
            } else {
              "GPS"
            }

            Text(
                "Location is $currentLocation",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.onTertiary,
                    shadow = Shadow(blurRadius = 4.0f)
                )
            )
          }
        }
    )
  }
}
