package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class NavigationMapPuckStyle(
    val dotFillColorCurrentLocation: Color = Color(0xFF3583DD),
    val dotFillColorOldLocation: Color = Color(0xFF3583DD),
    val dotStrokeColor: Color = Color.White,
    val shadowColor: Color = Color.Black.copy(alpha = 0.2f),
    val accuracyStrokeColor: Color = Color(0xFF3583DD),
    val accuracyFillColor: Color = Color(0xFF3583DD).copy(alpha = 0.16f),
    val bearingColor: Color = Color(0xFF0F5FB8),
    val dotRadius: Dp = 6.dp,
    val dotStrokeWidth: Dp = 3.dp,
    val showBearing: Boolean = true,
    val showBearingAccuracy: Boolean = false,
) {
  companion object {
    fun Default() = NavigationMapPuckStyle()
  }
}
