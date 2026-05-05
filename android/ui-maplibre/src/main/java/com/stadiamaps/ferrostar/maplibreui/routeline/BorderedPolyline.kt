package com.stadiamaps.ferrostar.maplibreui.routeline

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import uniffi.ferrostar.GeographicCoordinate

@Composable
fun BorderedPolyline(
    points: List<GeographicCoordinate>,
    idPrefix: String = "ferrostar-route",
    color: Color = Color(0xFF3583DD),
    opacity: Float = 1.0f,
    borderColor: Color = Color.White,
    borderOpacity: Float = 1.0f,
    lineWidth: Float = 10f,
    borderWidth: Float = 3f,
) {
  val routeJson = lineStringFeatureCollectionJson(points) ?: return
  val routeSource = rememberGeoJsonSource(GeoJsonData.JsonString(routeJson))

  LineLayer(
      id = "$idPrefix-border",
      source = routeSource,
      color = const(borderColor.copy(alpha = borderOpacity)),
      width = const((lineWidth + borderWidth * 2f).dp),
      cap = const(LineCap.Round),
      join = const(LineJoin.Round),
  )
  LineLayer(
      id = "$idPrefix-fill",
      source = routeSource,
      color = const(color.copy(alpha = opacity)),
      width = const(lineWidth.dp),
      cap = const(LineCap.Round),
      join = const(LineJoin.Round),
  )
}
