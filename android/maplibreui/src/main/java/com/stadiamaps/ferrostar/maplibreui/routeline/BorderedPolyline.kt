package com.stadiamaps.ferrostar.maplibreui.routeline

import androidx.compose.runtime.Composable
import com.maplibre.compose.symbols.Polyline
import org.maplibre.android.geometry.LatLng

@Composable
fun BorderedPolyline(
    points: List<LatLng>,
    zIndex: Int = 1,
    color: String = "#3583dd",
    opacity: Float = 1.0f,
    borderColor: String = "#ffffff",
    borderOpacity: Float = 1.0f,
    lineWidth: Float = 10f,
    borderWidth: Float = 3f
) {
  // Border
  Polyline(
      points = points,
      color = borderColor,
      opacity = borderOpacity,
      lineWidth = lineWidth + borderWidth * 2f,
      zIndex = zIndex,
  )
  // Body
  Polyline(
      points = points,
      color = color,
      opacity = opacity,
      lineWidth = lineWidth,
      zIndex = zIndex,
  )
}
