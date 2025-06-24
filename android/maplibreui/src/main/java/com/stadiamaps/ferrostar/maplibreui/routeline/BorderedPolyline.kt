package com.stadiamaps.ferrostar.maplibreui.routeline

import androidx.compose.runtime.Composable
import com.maplibre.compose.symbols.Polyline
import org.maplibre.android.geometry.LatLng

@Composable
fun BorderedPolyline(
    points: List<LatLng>,
    zIndex: Int = 1,
    color: String = "#3583dd",
    borderColor: String = "#ffffff",
    lineWidth: Float = 10f,
    borderWidth: Float = 3f
) {
  Polyline(
      points = points,
      color = borderColor,
      lineWidth = lineWidth + borderWidth * 2f,
      zIndex = zIndex,
  )
  Polyline(
      points = points,
      color = color,
      lineWidth = lineWidth,
      zIndex = zIndex,
  )
}
