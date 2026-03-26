package com.stadiamaps.ferrostar.auto

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.AppModule
import com.stadiamaps.ferrostar.DemoNavigationViewModel
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.ui.maplibre.car.app.CarAppNavigationView
import com.stadiamaps.ferrostar.ui.maplibre.car.app.runtime.SurfaceAreaTracker
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.config.withSpeedLimitStyle
import com.stadiamaps.ferrostar.composeui.views.components.speedlimit.SignageStyle
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationCameraOptions
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationMapState
import kotlinx.serialization.json.buildJsonObject
import kotlin.math.min
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point

@Composable
fun DemoNavigationView(
    viewModel: DemoNavigationViewModel = AppModule.viewModel,
    navigationMapState: NavigationMapState,
    navigationCameraOptions: NavigationCameraOptions,
    surfaceAreaTracker: SurfaceAreaTracker? = null,
) {
    CarAppNavigationView(
        modifier = Modifier.fillMaxSize(),
        styleUrl = AppModule.mapStyleUrl,
        navigationMapState = navigationMapState,
        navigationCameraOptions = navigationCameraOptions,
        viewModel = viewModel,
        config = VisualNavigationViewConfig.Default()
            .withSpeedLimitStyle(SignageStyle.MUTCD),
        surfaceAreaTracker = surfaceAreaTracker,
    ) { uiState ->
        DemoCarLocationOverlay(uiState)
    }
}

@Composable
@MaplibreComposable
private fun DemoCarLocationOverlay(uiState: NavigationUiState) {
    val location = uiState.location ?: return
    val locationSource =
        rememberGeoJsonSource(
            GeoJsonData.Features(
                FeatureCollection(
                    Feature(
                        geometry = Point(location.coordinates.lng, location.coordinates.lat),
                        properties = buildJsonObject {},
                    )
                )
            ),
        )

    CircleLayer(
        id = "demo-car-location-dot",
        source = locationSource,
        color = const(Color.Blue),
        radius = const(10.dp),
    )

    if (location.horizontalAccuracy > 15) {
        CircleLayer(
            id = "demo-car-location-accuracy",
            source = locationSource,
            color = const(Color.Blue),
            opacity = const(0.2f),
            radius = const(min(location.horizontalAccuracy.toFloat(), 150f).dp),
        )
    }
}
