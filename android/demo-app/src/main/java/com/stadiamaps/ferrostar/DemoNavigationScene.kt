package com.stadiamaps.ferrostar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.stadiamaps.ferrostar.composeui.config.NavigationViewComponentBuilder
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.config.withCustomOverlayView
import com.stadiamaps.ferrostar.composeui.config.withSpeedLimitStyle
import com.stadiamaps.ferrostar.composeui.runtime.KeepScreenOnDisposableEffect
import com.stadiamaps.ferrostar.composeui.views.components.speedlimit.SignageStyle
import com.stadiamaps.ferrostar.maplibreui.NavigationMapClickResult
import com.stadiamaps.ferrostar.maplibreui.runtime.rememberNavigationMapState
import com.stadiamaps.ferrostar.maplibreui.views.DynamicallyOrientingNavigationView
import com.stadiamaps.ferrostar.ui.DestinationSelectionCameraEffect
import com.stadiamaps.ferrostar.ui.DestinationSelectionBottomSheet
import kotlinx.serialization.json.buildJsonObject
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import uniffi.ferrostar.GeographicCoordinate

@Composable
fun DemoNavigationScene(
    viewModel: DemoNavigationViewModel = AppModule.viewModel
) {
  // Keeps the screen on at consistent brightness while this Composable is in the view hierarchy.
  KeepScreenOnDisposableEffect()

  val context = LocalContext.current

  // Get location permissions.
  // NOTE: This is NOT a robust suggestion for how to get permissions in a production app.
  // This is simply minimal sample code in as few lines as possible.
  val allPermissions =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION)
      } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
      }

  val permissionsLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
          permissions ->
        when {
          permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
            viewModel.setLocationPermissions(true)
          }
          permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
            // TODO: Probably alert the user that this is unusable for navigation
          }
          // TODO: Foreground service permissions; we should block access until approved on API 34+
          else -> {
            // TODO
          }
        }
      }

  LaunchedEffect(Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED) {
      viewModel.setLocationPermissions(true)
    } else {
      permissionsLauncher.launch(allPermissions)
    }
  }
  val sceneState by viewModel.sceneState.collectAsState()
  val navigationMapState = rememberNavigationMapState()
  var destinationPreviewTopPaddingPx by remember { mutableStateOf(0) }
  DestinationSelectionCameraEffect(
      selectedDestination = sceneState.selectedDestination,
      destinationSheetHeightPx = sceneState.destinationSheetHeightPx,
      topOverlayBottomPx = destinationPreviewTopPaddingPx,
      navigationMapState = navigationMapState,
  )

  DynamicallyOrientingNavigationView(
      modifier = Modifier.fillMaxSize(),
      baseStyle = BaseStyle.Uri(AppModule.mapStyleUrl),
      navigationMapState = navigationMapState,
      viewModel = viewModel,
      config = VisualNavigationViewConfig.Default().withSpeedLimitStyle(SignageStyle.MUTCD),
      views =
          NavigationViewComponentBuilder.Default()
              .withCustomOverlayView(
                  customOverlayView = { modifier ->
                    NotNavigatingOverlay(
                        modifier = modifier,
                        viewModel = viewModel,
                        navigationMapState = navigationMapState,
                        onTopOverlayBottomChanged = { destinationPreviewTopPaddingPx = it },
                    )
                  },
              ),
      onTapExit = { viewModel.stopNavigation() },
      onMapLongClick = { position, screenPosition ->
        Log.d(
            "DemoNavigationScene",
            "Long press at lat=${position.lat}, lng=${position.lng}, screen=$screenPosition",
        )
        viewModel.selectDestination(position)
        NavigationMapClickResult.Consume
      },
  ) {
    DemoDroppedPinOverlay(sceneState.droppedPin)
  }

  if (sceneState.isDestinationSheetVisible) {
    sceneState.selectedDestination?.let { destination ->
      DestinationSelectionBottomSheet(
          destination = destination,
          onClose = { viewModel.clearSelectedDestination() },
          onStartNavigation = { viewModel.startSelectedDestinationNavigation() },
          onSheetHeightChanged = viewModel::setDestinationSheetHeight,
      )
    }
  }
}

@Composable
@MaplibreComposable
private fun DemoDroppedPinOverlay(droppedPin: GeographicCoordinate?) {
  val pinFeatureCollection = droppedPinFeatureCollectionOrNull(droppedPin) ?: return
  val pointSource = rememberGeoJsonSource(GeoJsonData.Features(pinFeatureCollection))

  CircleLayer(
      id = "demo-dropped-pin",
      source = pointSource,
      color = const(Color.Red),
      radius = const(10.dp),
      strokeColor = const(Color.White),
      strokeWidth = const(2.dp),
  )
}

internal fun droppedPinFeatureCollectionOrNull(pin: GeographicCoordinate?) =
    pin?.let {
      droppedPinFeatureCollection(it)
    }

internal fun droppedPinFeatureCollection(pin: GeographicCoordinate) =
    FeatureCollection(
        Feature(
            geometry = Point(longitude = pin.lng, latitude = pin.lat),
            properties = buildJsonObject {},
        )
    )
