package com.stadiamaps.ferrostar.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.DestinationSelection
import com.stadiamaps.ferrostar.DestinationSelectionOrigin
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationCameraMode
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationMapState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.spatialk.geojson.Position
import uniffi.ferrostar.GeographicCoordinate

@Composable
fun DestinationSelectionCameraEffect(
    selectedDestination: DestinationSelection?,
    destinationSheetHeightPx: Int,
    topOverlayBottomPx: Int,
    navigationMapState: NavigationMapState,
) {
  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current
  val topPadding =
      with(density) {
        topOverlayBottomPx.toDp()
            .takeIf { it > 0.dp }
            ?.plus(24.dp)
            ?: 0.dp
      }
  val bottomPadding = with(density) { destinationSheetHeightPx.toDp() + 24.dp }
  var previewedDestination by remember { mutableStateOf<DestinationSelection?>(null) }

  LaunchedEffect(
      selectedDestination,
      destinationSheetHeightPx,
      topOverlayBottomPx,
      layoutDirection,
  ) {
    val destination = selectedDestination
    if (destination == null) {
      previewedDestination = null
      return@LaunchedEffect
    }
    if (destinationSheetHeightPx <= 0) {
      return@LaunchedEffect
    }

    if (previewedDestination != destination) {
      val zoom =
          if (destination.origin == DestinationSelectionOrigin.SearchResult) {
            15.5
          } else {
            maxOf(navigationMapState.cameraState.position.zoom, 15.0)
          }
      navigationMapState.animateDestinationPreview(
          coordinate = destination.coordinate,
          zoom = zoom,
          topPadding = topPadding,
          bottomPadding = bottomPadding,
          layoutDirection = layoutDirection,
      )
      previewedDestination = destination
    } else {
      navigationMapState.updateDestinationPreviewPadding(
          topPadding = topPadding,
          bottomPadding = bottomPadding,
          layoutDirection = layoutDirection,
      )
    }
  }
}

private fun NavigationMapState.updateDestinationPreviewPadding(
    topPadding: Dp,
    bottomPadding: Dp,
    layoutDirection: LayoutDirection,
) {
  cameraState.position =
      cameraState.position.copy(
          padding =
              navigationCameraOptions.browsingPadding.withDestinationPreviewPadding(
                  topPadding = topPadding,
                  bottomPadding = bottomPadding,
                  layoutDirection = layoutDirection,
              ),
        )
}

private suspend fun NavigationMapState.animateDestinationPreview(
    coordinate: GeographicCoordinate,
    zoom: Double,
    topPadding: Dp,
    bottomPadding: Dp,
    layoutDirection: LayoutDirection,
    duration: Duration = 1200.milliseconds,
) {
  cameraMode = NavigationCameraMode.FREE
  cameraState.animateTo(
      finalPosition =
          CameraPosition(
              target = Position(longitude = coordinate.lng, latitude = coordinate.lat),
              zoom = zoom,
              tilt = 0.0,
              bearing = 0.0,
              padding =
                  navigationCameraOptions.browsingPadding.withDestinationPreviewPadding(
                      topPadding = topPadding,
                      bottomPadding = bottomPadding,
                      layoutDirection = layoutDirection,
                  ),
          ),
      duration = duration,
  )
}

private fun PaddingValues.withDestinationPreviewPadding(
    topPadding: Dp,
    bottomPadding: Dp,
    layoutDirection: LayoutDirection,
): PaddingValues =
    PaddingValues(
        start = calculateLeftPadding(layoutDirection),
        top = calculateTopPadding() + topPadding,
        end = calculateRightPadding(layoutDirection),
        bottom = calculateBottomPadding() + bottomPadding,
    )
