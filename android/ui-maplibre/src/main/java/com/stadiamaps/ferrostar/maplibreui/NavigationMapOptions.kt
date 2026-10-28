package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.map.CameraConstraints
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.RenderOptions
import org.maplibre.compose.overlay.LocalViewportInsets
import org.maplibre.compose.overlay.MapOverlay

/**
 * MapLibre settings for a Ferrostar navigation map.
 *
 * @param cameraConstraints The zoom, pitch, and bounds limits of the camera.
 * @param renderOptions The frame rate, tile level of detail, and debug overlays of the map.
 * @param interactions The camera movements that gestures can make. The navigation map view replaces
 *   the click and long click callbacks with its own `onMapClick` and `onMapLongClick`.
 * @param uiOptions The gestures, keys, and other input that control the map.
 * @param overlay The controls drawn over the map. The default shows the MapLibre logo and the
 *   attribution button.
 * @param overlayPadding The space between the edges of the map and [overlay]. The overlay reads it
 *   as [LocalViewportInsets]. The phone and tablet navigation views replace it with the padding
 *   that keeps the overlay clear of the navigation UI.
 */
@Immutable
data class NavigationMapOptions(
    val cameraConstraints: CameraConstraints = CameraConstraints.Standard,
    val renderOptions: RenderOptions = RenderOptions.Standard,
    val interactions: MapInteractions = MapInteractions.Standard,
    val uiOptions: MapUiOptions = MapUiOptions.Standard,
    val overlay: MapOverlay = MapOverlay.AttributionOnly,
    val overlayPadding: PaddingValues = PaddingValues(0.dp),
)
