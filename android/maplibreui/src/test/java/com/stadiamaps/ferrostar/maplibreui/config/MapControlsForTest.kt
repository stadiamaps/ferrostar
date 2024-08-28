package com.stadiamaps.ferrostar.maplibreui.config

import android.view.Gravity
import org.junit.Assert.assertEquals
import org.junit.Test

class MapControlsForTest {

  @Test
  fun testMapControlsForLandscapeExpanded() {
    val isLandscape = true
    val isArrivalExpanded = true
    val result = mapControlsFor(isLandscape, isArrivalExpanded)

    assertEquals(Gravity.BOTTOM or Gravity.END, result.attribution?.gravity)
    assertEquals(270, result.attribution?.margins?.end)
    assertEquals(32, result.attribution?.margins?.bottom)
    assertEquals(false, result.compass?.enabled)
    assertEquals(Gravity.BOTTOM or Gravity.END, result.logo?.gravity)
    assertEquals(32, result.logo?.margins?.end)
    assertEquals(32, result.logo?.margins?.bottom)
  }

  @Test
  fun testMapControlsForLandscape() {
    val isLandscape = true
    val isArrivalExpanded = false
    val result = mapControlsFor(isLandscape, isArrivalExpanded)

    assertEquals(Gravity.BOTTOM or Gravity.END, result.attribution?.gravity)
    assertEquals(270, result.attribution?.margins?.end)
    assertEquals(32, result.attribution?.margins?.bottom)
    assertEquals(false, result.compass?.enabled)
    assertEquals(Gravity.BOTTOM or Gravity.END, result.logo?.gravity)
    assertEquals(32, result.logo?.margins?.end)
    assertEquals(32, result.logo?.margins?.bottom)
  }

  @Test
  fun testMapControlsForPortraitExpanded() {
    val isLandscape = false
    val isArrivalExpanded = true
    val result = mapControlsFor(isLandscape, isArrivalExpanded)

    assertEquals(Gravity.BOTTOM or Gravity.END, result.attribution?.gravity)
    assertEquals(270, result.attribution?.margins?.end)
    assertEquals(288, result.attribution?.margins?.bottom)
    assertEquals(false, result.compass?.enabled)
    assertEquals(Gravity.BOTTOM or Gravity.END, result.logo?.gravity)
    assertEquals(32, result.logo?.margins?.end)
    assertEquals(288, result.logo?.margins?.bottom)
  }

  @Test
  fun testMapControlsForPortrait() {
    val isLandscape = false
    val isArrivalExpanded = false
    val result = mapControlsFor(isLandscape, isArrivalExpanded)

    assertEquals(Gravity.BOTTOM or Gravity.END, result.attribution?.gravity)
    assertEquals(270, result.attribution?.margins?.end)
    assertEquals(224, result.attribution?.margins?.bottom)
    assertEquals(false, result.compass?.enabled)
    assertEquals(Gravity.BOTTOM or Gravity.END, result.logo?.gravity)
    assertEquals(32, result.logo?.margins?.end)
    assertEquals(224, result.logo?.margins?.bottom)
  }
}
