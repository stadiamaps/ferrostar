package com.stadiamaps.ferrostar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.stadiamaps.autocomplete.AutocompleteSearch
import com.stadiamaps.autocomplete.center
import com.stadiamaps.ferrostar.composeui.views.components.controls.NavigationUIButton
import com.stadiamaps.ferrostar.composeui.views.components.gridviews.InnerGridView
import com.stadiamaps.ferrostar.core.location.toAndroidLocation
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationMapState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotNavigatingOverlay(
    modifier: Modifier = Modifier,
    viewModel: DemoNavigationViewModel,
    navigationMapState: NavigationMapState,
    onTopOverlayBottomChanged: (Int) -> Unit = {},
) {
  val location by viewModel.location.collectAsState()
  val isSimulating by viewModel.simulated.collectAsState()
  val uiState by viewModel.navigationUiState.collectAsState()
  val stadiaApiKey = AppModule.stadiaApiKey

  LaunchedEffect(stadiaApiKey) {
    if (stadiaApiKey == null) {
      onTopOverlayBottomChanged(0)
    }
  }

  if (!uiState.isNavigating()) {
    InnerGridView(
        modifier = modifier.fillMaxSize().padding(bottom = 16.dp, top = 16.dp),
        topCenter = {
          stadiaApiKey?.let { apiKey ->
            Box(
                modifier =
                    Modifier.onGloballyPositioned { coordinates ->
                      onTopOverlayBottomChanged(coordinates.boundsInRoot().bottom.roundToInt())
                    }
            ) {
              AutocompleteSearch(
                  apiKey = apiKey,
                  userLocation = location?.toAndroidLocation()
              ) { feature ->
                feature.center()?.let { center ->
                  viewModel.selectDestination(
                      location = center,
                      label = feature.properties.name,
                      origin = DestinationSelectionOrigin.SearchResult,
                  )
                }
              }
            }
          }
        },
        centerEnd = {
          NavigationUIButton(
              onClick = { navigationMapState.recenter(isNavigating = false) },
              buttonSize = DpSize(48.dp, 48.dp),
          ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = stringResource(R.string.center_on_my_location),
            )
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
