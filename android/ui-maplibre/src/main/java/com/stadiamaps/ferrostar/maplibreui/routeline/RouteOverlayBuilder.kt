package com.stadiamaps.ferrostar.maplibreui.routeline

import androidx.compose.runtime.Composable
import com.maplibre.compose.ramani.MapLibreComposable
import com.stadiamaps.ferrostar.core.NavigationUiState
import org.maplibre.android.geometry.LatLng

/**
 * A Route Overlay (Polyline) Builder with sensible defaults - showing the full Navigation Route
 * Geometry.
 *
 * This banner view includes the default [BorderedPolyline] to display the full supplied Route
 * Geometry. Custom implementations to the appearance/functionality of the Route Overlay can be
 * achieved by passing a custom implementation of the [navigationPath] parameter, using the
 * [NavigationUiState] to access the Route Geometry.
 */
data class RouteOverlayBuilder(
    internal val navigationPath:
        @Composable
        @MapLibreComposable
        (uiState: NavigationUiState) -> Unit
) {
  companion object {
    fun Default() =
        RouteOverlayBuilder(
            navigationPath = { uiState ->
              uiState.routeGeometry?.let { geometry ->
                BorderedPolyline(points = geometry.map { LatLng(it.lat, it.lng) }, zIndex = 0)
              }
            })
  }
}
