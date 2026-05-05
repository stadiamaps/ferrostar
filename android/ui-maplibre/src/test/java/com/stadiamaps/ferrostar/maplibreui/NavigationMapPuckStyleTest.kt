package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationMapPuckStyleTest {
  @Test
  fun defaultPuckStyleMatchesFerrostarColorsAndSizes() {
    val style = NavigationMapPuckStyle()

    assertEquals(Color(0xFF3583DD), style.dotFillColorCurrentLocation)
    assertEquals(Color(0xFF0F5FB8), style.bearingColor)
    assertEquals(7.dp, style.dotRadius)
    assertEquals(3.dp, style.dotStrokeWidth)
    assertTrue(style.showBearing)
    assertFalse(style.showBearingAccuracy)
  }
}
