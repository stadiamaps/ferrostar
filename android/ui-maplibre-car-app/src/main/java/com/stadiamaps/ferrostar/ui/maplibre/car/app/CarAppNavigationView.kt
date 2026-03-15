package com.stadiamaps.ferrostar.ui.maplibre.car.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.maplibre.compose.settings.AttributionSettings
import com.maplibre.compose.settings.CompassSettings
import com.maplibre.compose.settings.LogoSettings
import com.maplibre.compose.settings.MapControls
import com.stadiamaps.ferrostar.ui.maplibre.car.app.runtime.SurfaceAreaTracker
import com.stadiamaps.ferrostar.ui.maplibre.car.app.runtime.screenSurfaceState
import com.stadiamaps.ferrostar.ui.maplibre.car.app.runtime.surfaceStableFractionalPadding
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.views.components.CurrentRoadNameView
import com.stadiamaps.ferrostar.composeui.views.components.speedlimit.SpeedLimitView
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.maplibreui.NavigationMapView
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault
import com.stadiamaps.ferrostar.maplibreui.routeline.RouteOverlayBuilder
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationMapViewCamera

/**
 * A navigation view designed for Android Auto car displays.
 *
 * Renders a [NavigationMapView] with speed limit and road name overlays positioned within the
 * stable area of the car display surface (the area not covered by the NavigationTemplate's chrome).
 */
@Composable
fun CarAppNavigationView(
    modifier: Modifier,
    styleUrl: String,
    camera: MutableState<MapViewCamera> = rememberSaveableMapViewCamera(),
    navigationCamera: MapViewCamera = navigationMapViewCamera(),
    viewModel: NavigationViewModel,
    locationRequestProperties: LocationRequestProperties =
        LocationRequestProperties.NavigationDefault(),
    config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
    routeOverlayBuilder: RouteOverlayBuilder = RouteOverlayBuilder.Default(),
    surfaceAreaTracker: SurfaceAreaTracker? = null,
    mapContent: @Composable @MapLibreComposable() ((NavigationUiState) -> Unit)? = null,
) {
  val uiState by viewModel.navigationUiState.collectAsState()
  val mapControls = remember {
    mutableStateOf(
        MapControls(
            attribution = AttributionSettings(enabled = false),
            compass = CompassSettings(enabled = false),
            logo = LogoSettings(enabled = false)))
  }

  val surfaceArea by surfaceAreaTracker
      ?.let { screenSurfaceState(it) }
      ?: remember { mutableStateOf(null) }

  val gridPadding = surfaceStableFractionalPadding(surfaceArea?.compositeArea)

  // Wrap mapContent to also wire surface gesture handling inside the MapLibreComposable context.
  val wrappedContent: (@Composable @MapLibreComposable (NavigationUiState) -> Unit)? =
      if (surfaceAreaTracker != null || mapContent != null) {
        { uiState ->
          surfaceAreaTracker?.rememberSurfaceArea()
          mapContent?.invoke(uiState)
        }
      } else null

  Box(modifier) {
    NavigationMapView(
        styleUrl,
        camera,
        uiState = uiState,
        mapControls = mapControls,
        locationRequestProperties = locationRequestProperties,
        routeOverlayBuilder = routeOverlayBuilder,
        onMapReadyCallback = {
          // No definition
        },
        content = wrappedContent
    )

    Box(
        modifier = Modifier.fillMaxSize()
            .padding(gridPadding)
    ) {
      // Speed limit in top-start
      uiState.currentAnnotation?.speedLimit?.let { speedLimit ->
          config.speedLimitStyle?.let { speedLimitStyle ->
              SpeedLimitView(
                  speedLimit = speedLimit,
                  signageStyle = speedLimitStyle,
                  modifier = Modifier.align(Alignment.TopStart))
          }
      }

      // Road name at bottom-center
      uiState.currentStepRoadName?.let { roadName ->
        CurrentRoadNameView(
            currentRoadName = roadName,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
      }
    }
  }
}
