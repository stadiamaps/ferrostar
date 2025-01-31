package com.stadiamaps.ferrostar.maplibreui.routeline

import androidx.compose.runtime.Composable
import com.mapbox.mapboxsdk.geometry.LatLng
import com.maplibre.compose.ramani.MapLibreComposable
import com.stadiamaps.ferrostar.core.NavigationUiState

data class NavigationPathBuilder(
    internal val navigationPath:
        @Composable
        @MapLibreComposable
        (uiState: NavigationUiState) -> Unit
) {
  companion object {
    fun Default() =
        NavigationPathBuilder(
            navigationPath = { uiState ->
              uiState.routeGeometry?.let { geometry ->
                BorderedPolyline(points = geometry.map { LatLng(it.lat, it.lng) }, zIndex = 0)
              }
            })
  }
}
