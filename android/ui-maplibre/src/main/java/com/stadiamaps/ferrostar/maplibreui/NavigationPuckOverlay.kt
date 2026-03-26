package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.core.NavigationUiState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.compose.expressions.dsl.asNumber
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.CirclePitchAlignment
import org.maplibre.compose.expressions.value.IconPitchAlignment
import org.maplibre.compose.expressions.value.IconRotationAlignment
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point

internal fun shouldRenderNavigationPuck(uiState: NavigationUiState): Boolean =
    uiState.isNavigating() && uiState.location != null

internal fun navigationPuckBearingDegrees(
    currentBearing: Double?,
    lastKnownBearing: Double
): Double =
    currentBearing ?: lastKnownBearing

internal fun navigationPuckFeatureCollection(
    longitude: Double,
    latitude: Double,
    bearingDegrees: Double,
): FeatureCollection<Point, JsonObject> =
    FeatureCollection(
        Feature(
            geometry = Point(longitude, latitude),
            properties =
                buildJsonObject {
                  put("bearing", bearingDegrees)
                },
        )
    )

@Composable
@MaplibreComposable
internal fun NavigationPuckOverlay(
    longitude: Double,
    latitude: Double,
    bearingDegrees: Double,
    style: NavigationMapPuckStyle,
) {
  val source =
      rememberGeoJsonSource(
          GeoJsonData.Features(
              navigationPuckFeatureCollection(longitude, latitude, bearingDegrees),
          ),
      )
  val arrowPainter = rememberNavigationPuckArrowPainter(style.dotFillColorCurrentLocation)
  val puckRadius = 23.dp
  val puckShadowRadius = 32.dp
  val arrowSize = 23.dp

  CircleLayer(
      id = "ferrostar-navigation-puck-shadow",
      source = source,
      color = const(Color.Black),
      radius = const(puckShadowRadius),
      opacity = const(0.1f),
      pitchAlignment = const(CirclePitchAlignment.Map),
  )

  CircleLayer(
      id = "ferrostar-navigation-puck-background",
      source = source,
      color = const(Color.White),
      radius = const(puckRadius),
      strokeColor = const(Color(0xFFE5E5EA)),
      strokeWidth = const(0.75.dp),
      pitchAlignment = const(CirclePitchAlignment.Map),
  )

  SymbolLayer(
      id = "ferrostar-navigation-puck-arrow",
      source = source,
      iconImage =
          image(
              arrowPainter,
              size = DpSize(arrowSize, arrowSize),
              drawAsSdf = false,
          ),
      iconAnchor = const(SymbolAnchor.Center),
      iconRotate = feature["bearing"].asNumber(const(0f)),
      iconPitchAlignment = const(IconPitchAlignment.Map),
      iconRotationAlignment = const(IconRotationAlignment.Map),
      iconAllowOverlap = const(true),
      iconIgnorePlacement = const(true),
  )
}

@Composable
private fun rememberNavigationPuckArrowPainter(color: Color): VectorPainter =
    rememberVectorPainter(
        remember(color) {
          ImageVector.Builder(
              name = "ferrostar_navigation_puck_arrow",
              defaultWidth = 20.dp,
              defaultHeight = 20.dp,
              viewportWidth = 20f,
              viewportHeight = 20f,
          )
              .apply {
                path(fill = androidx.compose.ui.graphics.SolidColor(color)) {
                  moveTo(10f, 1.6f)
                  lineTo(17.8f, 17.2f)
                  lineTo(10f, 12.6f)
                  lineTo(2.2f, 17.2f)
                  close()
                }
              }
              .build()
        },
    )
