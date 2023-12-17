package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.mapbox.mapboxsdk.geometry.LatLng
import com.stadiamaps.ferrostar.core.NavigationViewModel
import org.ramani.compose.Circle
import org.ramani.compose.MapLibre
import org.ramani.compose.Polyline

@Composable
fun NavigationMapView(
    viewModel: NavigationViewModel
) {
    val uiState = viewModel.uiState.collectAsState()

    MapLibre(modifier = Modifier.fillMaxSize()) {
        Circle(
            center = LatLng(
                uiState.value.snappedLocation.coordinates.lat,
                uiState.value.snappedLocation.coordinates.lng
            ), radius = 10f, color = "Blue"
        )
        Polyline(
            points = uiState.value.routeGeometry.map { LatLng(it.lat, it.lng) },
            color = "Red",
            lineWidth = 5f
        )
    }
}
