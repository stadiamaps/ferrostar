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
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.CirclePitchAlignment
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import uniffi.ferrostar.GeographicCoordinate

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
        DemoRouteEndpointsOverlay(uiState)
    }
}

@Composable
@MaplibreComposable
private fun DemoRouteEndpointsOverlay(uiState: NavigationUiState) {
    val route = uiState.routeGeometry ?: return
    val start = route.firstOrNull() ?: return
    val end = route.lastOrNull() ?: return

    val startSource =  rememberGeoJsonSource(
        GeoJsonData.Features(start.toRouteEndpointFeatureCollection())
    )
    val endSource = rememberGeoJsonSource(
        GeoJsonData.Features(end.toRouteEndpointFeatureCollection())
    )

    CircleLayer(
        id = "demo-route-start",
        source = startSource,
        color = const(Color.Gray),
        radius = const(10.dp),
        strokeColor = const(Color.White),
        strokeWidth = const(2.dp),
        pitchAlignment = const(CirclePitchAlignment.Map),
        opacity = const(0.6f),
    )

    CircleLayer(
        id = "demo-route-end",
        source = endSource,
        color = const(Color.Green),
        radius = const(10.dp),
        strokeColor = const(Color.White),
        strokeWidth = const(10.dp),
        pitchAlignment = const(CirclePitchAlignment.Map),
        opacity = const(0.6f),
    )
}

private fun GeographicCoordinate.toRouteEndpointFeatureCollection() =
    FeatureCollection(
        Feature(
            geometry = Point(
                longitude = this.lng,
                latitude = this.lat,
            ),
            properties = buildJsonObject {},
        )
    )
