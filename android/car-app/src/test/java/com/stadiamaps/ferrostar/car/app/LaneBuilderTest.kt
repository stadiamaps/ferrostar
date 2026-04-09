package com.stadiamaps.ferrostar.car.app

import androidx.car.app.navigation.model.LaneDirection
import com.stadiamaps.ferrostar.car.app.template.models.asLaneShape
import org.junit.Assert.assertEquals
import org.junit.Test
import uniffi.ferrostar.LaneInfo

class LaneBuilderTest {

  @Test
  fun `uturn maps to SHAPE_U_TURN_LEFT`() {
    assertEquals(LaneDirection.SHAPE_U_TURN_LEFT, LaneInfo.asLaneShape("uturn"))
  }

  @Test
  fun `sharp right maps to SHAPE_SHARP_RIGHT`() {
    assertEquals(LaneDirection.SHAPE_SHARP_RIGHT, LaneInfo.asLaneShape("sharp right"))
  }

  @Test
  fun `right maps to SHAPE_NORMAL_RIGHT`() {
    assertEquals(LaneDirection.SHAPE_NORMAL_RIGHT, LaneInfo.asLaneShape("right"))
  }

  @Test
  fun `slight right maps to SHAPE_SLIGHT_RIGHT`() {
    assertEquals(LaneDirection.SHAPE_SLIGHT_RIGHT, LaneInfo.asLaneShape("slight right"))
  }

  @Test
  fun `straight maps to SHAPE_STRAIGHT`() {
    assertEquals(LaneDirection.SHAPE_STRAIGHT, LaneInfo.asLaneShape("straight"))
  }

  @Test
  fun `slight left maps to SHAPE_SLIGHT_LEFT`() {
    assertEquals(LaneDirection.SHAPE_SLIGHT_LEFT, LaneInfo.asLaneShape("slight left"))
  }

  @Test
  fun `left maps to SHAPE_NORMAL_LEFT`() {
    assertEquals(LaneDirection.SHAPE_NORMAL_LEFT, LaneInfo.asLaneShape("left"))
  }

  @Test
  fun `sharp left maps to SHAPE_SHARP_LEFT`() {
    assertEquals(LaneDirection.SHAPE_SHARP_LEFT, LaneInfo.asLaneShape("sharp left"))
  }

  @Test
  fun `unknown indication maps to SHAPE_UNKNOWN`() {
    assertEquals(LaneDirection.SHAPE_UNKNOWN, LaneInfo.asLaneShape("bogus"))
  }

  @Test
  fun `empty string maps to SHAPE_UNKNOWN`() {
    assertEquals(LaneDirection.SHAPE_UNKNOWN, LaneInfo.asLaneShape(""))
  }
}
