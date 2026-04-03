package com.stadiamaps.ferrostar.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun DestinationSelectionCameraPreviewEffect(
    selectedDestination: DestinationSelection?,
    destinationSheetHeightPx: Int,
    navigationMapState: NavigationMapState,
) {
  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current
  val bottomPadding = with(density) { destinationSheetHeightPx.toDp() + 24.dp }

  LaunchedEffect(selectedDestination) {
    val destination = selectedDestination ?: return@LaunchedEffect
    val zoom =
        if (destination.origin == DestinationSelectionOrigin.SearchResult) {
          15.5
        } else {
          maxOf(navigationMapState.cameraState.position.zoom, 15.0)
        }
    navigationMapState.animateDestinationPreview(
        coordinate = destination.coordinate,
        zoom = zoom,
        bottomPadding = bottomPadding,
        layoutDirection = layoutDirection,
    )
  }
}

@Composable
fun DestinationSelectionCameraPaddingEffect(
    selectedDestination: DestinationSelection?,
    destinationSheetHeightPx: Int,
    navigationMapState: NavigationMapState,
) {
  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current
  val bottomPadding = with(density) { destinationSheetHeightPx.toDp() + 24.dp }

  LaunchedEffect(destinationSheetHeightPx, layoutDirection) {
    if (selectedDestination == null || destinationSheetHeightPx <= 0) {
      return@LaunchedEffect
    }
    navigationMapState.cameraState.position =
        navigationMapState.cameraState.position.copy(
            padding =
                navigationMapState.navigationCameraOptions.browsingPadding.withAdditionalBottomPadding(
                    bottomPadding = bottomPadding,
                    layoutDirection = layoutDirection,
                ),
        )
  }
}

private suspend fun NavigationMapState.animateDestinationPreview(
    coordinate: GeographicCoordinate,
    zoom: Double,
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
                  navigationCameraOptions.browsingPadding.withAdditionalBottomPadding(
                      bottomPadding = bottomPadding,
                      layoutDirection = layoutDirection,
                  ),
          ),
      duration = duration,
  )
}

private fun PaddingValues.withAdditionalBottomPadding(
    bottomPadding: Dp,
    layoutDirection: LayoutDirection,
): PaddingValues =
    PaddingValues(
        start = calculateLeftPadding(layoutDirection),
        top = calculateTopPadding(),
        end = calculateRightPadding(layoutDirection),
        bottom = calculateBottomPadding() + bottomPadding,
    )
