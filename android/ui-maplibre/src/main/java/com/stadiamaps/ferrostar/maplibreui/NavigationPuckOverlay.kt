package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
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
import org.maplibre.compose.expressions.value.IconPitchAlignment
import org.maplibre.compose.expressions.value.IconRotationAlignment
import org.maplibre.compose.expressions.value.SymbolAnchor
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
  val puckPainter = rememberNavigationPuckPainter(style.dotFillColorCurrentLocation)
  val puckSize = 80.dp

  SymbolLayer(
      id = "ferrostar-navigation-puck",
      source = source,
      iconImage =
          image(
              value = puckPainter,
              size = DpSize(
                  width = puckSize,
                  height = puckSize,
              ),
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
private fun rememberNavigationPuckPainter(color: Color): Painter =
    remember(color) {
      object : Painter() {
        override val intrinsicSize: Size = Size.Unspecified

        override fun androidx.compose.ui.graphics.drawscope.DrawScope.onDraw() {
          val minDimension = size.minDimension
          val center = Offset(size.width / 2f, size.height / 2f)
          val haloRadius = minDimension * 0.5f
          val puckRadius = minDimension * 0.359375f
          val borderWidth = minDimension * 0.012f

          drawCircle(
              color = Color.Black.copy(alpha = 0.1f),
              radius = haloRadius,
              center = center,
          )
          drawCircle(
              color = Color.White,
              radius = puckRadius,
              center = center,
          )
          drawCircle(
              color = Color(0xFFE5E5EA),
              radius = puckRadius,
              center = center,
              style = Stroke(width = borderWidth),
          )
          drawPath(
              path = navigationArrowPath(size),
              color = color,
          )
        }
      }
    }

private fun navigationArrowPath(size: Size): Path {
  val minDimension = size.minDimension
  val centerX = size.width / 2f
  val centerY = size.height / 2f
  val arrowHeight = minDimension * 0.34f
  val arrowWidth = minDimension * 0.26f

  return Path().apply {
    moveTo(centerX, centerY - arrowHeight * 0.55f)
    lineTo(centerX + arrowWidth * 0.5f, centerY + arrowHeight * 0.35f)
    lineTo(centerX, centerY + arrowHeight * 0.1f)
    lineTo(centerX - arrowWidth * 0.5f, centerY + arrowHeight * 0.35f)
    close()
  }
}
