package com.stadiamaps.ferrostar.auto

import android.graphics.Rect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.symbols.Circle
import com.stadiamaps.ferrostar.AppModule
import com.stadiamaps.ferrostar.DemoNavigationViewModel
import com.stadiamaps.ferrostar.carapp.maplibreui.CarAppNavigationView
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.config.withSpeedLimitStyle
import com.stadiamaps.ferrostar.composeui.views.components.speedlimit.SignageStyle
import org.maplibre.android.geometry.LatLng
import kotlin.math.min

@Composable
fun DemoNavigationView(
    viewModel: DemoNavigationViewModel = AppModule.viewModel,
    camera: MutableState<MapViewCamera>,
    stableArea: Rect? = null,
) {
    // Note: you can also request location permission when launching your car app.
    // E.g. https://developer.android.com/training/cars/apps/library/request-permissions

    CarAppNavigationView(
        modifier = Modifier.fillMaxSize(),
        styleUrl = AppModule.mapStyleUrl,
        camera = camera,
        viewModel = viewModel,
        config = VisualNavigationViewConfig.Default()
            .withSpeedLimitStyle(SignageStyle.MUTCD),
        stableArea = stableArea
    ) { uiState ->
        // Trivial, if silly example of how to add your own overlay layers.
        // (Also incidentally highlights the lag inherent in MapLibre location tracking
        // as-is.)
        uiState.location?.let { location ->
            Circle(
                center = LatLng(location.coordinates.lat, location.coordinates.lng),
                radius = 10f,
                color = "Blue",
                zIndex = 3,
            )

            if (location.horizontalAccuracy > 15) {
                Circle(
                    center = LatLng(location.coordinates.lat, location.coordinates.lng),
                    radius = min(location.horizontalAccuracy.toFloat(), 150f),
                    color = "Blue",
                    opacity = 0.2f,
                    zIndex = 2,
                )
            }
        }
    }
}
