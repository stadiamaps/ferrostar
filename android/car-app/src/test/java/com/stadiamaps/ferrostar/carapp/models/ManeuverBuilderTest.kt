package com.stadiamaps.ferrostar.carapp.models

import androidx.car.app.navigation.model.LaneDirection
import androidx.car.app.navigation.model.Maneuver
import com.stadiamaps.ferrostar.carapp.template.models.isRoundaboutManeuverType
import com.stadiamaps.ferrostar.carapp.template.models.toCarManeuverType
import com.stadiamaps.ferrostar.carapp.template.models.toLaneShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uniffi.ferrostar.DrivingSide
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType

class ManeuverBuilderTest {

  @Test
  fun `turn left maps correctly`() {
    assertEquals(
        Maneuver.TYPE_TURN_NORMAL_LEFT, ManeuverType.TURN.toCarManeuverType(ManeuverModifier.LEFT))
  }

  @Test
  fun `turn right maps correctly`() {
    assertEquals(
        Maneuver.TYPE_TURN_NORMAL_RIGHT,
        ManeuverType.TURN.toCarManeuverType(ManeuverModifier.RIGHT))
  }

  @Test
  fun `turn sharp left maps correctly`() {
    assertEquals(
        Maneuver.TYPE_TURN_SHARP_LEFT,
        ManeuverType.TURN.toCarManeuverType(ManeuverModifier.SHARP_LEFT))
  }

  @Test
  fun `turn sharp right maps correctly`() {
    assertEquals(
        Maneuver.TYPE_TURN_SHARP_RIGHT,
        ManeuverType.TURN.toCarManeuverType(ManeuverModifier.SHARP_RIGHT))
  }

  @Test
  fun `turn slight left maps correctly`() {
    assertEquals(
        Maneuver.TYPE_TURN_SLIGHT_LEFT,
        ManeuverType.TURN.toCarManeuverType(ManeuverModifier.SLIGHT_LEFT))
  }

  @Test
  fun `turn slight right maps correctly`() {
    assertEquals(
        Maneuver.TYPE_TURN_SLIGHT_RIGHT,
        ManeuverType.TURN.toCarManeuverType(ManeuverModifier.SLIGHT_RIGHT))
  }

  @Test
  fun `u-turn maps correctly`() {
    assertEquals(
        Maneuver.TYPE_U_TURN_LEFT, ManeuverType.TURN.toCarManeuverType(ManeuverModifier.U_TURN))
  }

  @Test
  fun `straight maps correctly`() {
    assertEquals(
        Maneuver.TYPE_STRAIGHT, ManeuverType.TURN.toCarManeuverType(ManeuverModifier.STRAIGHT))
  }

  @Test
  fun `depart maps correctly`() {
    assertEquals(Maneuver.TYPE_DEPART, ManeuverType.DEPART.toCarManeuverType(null))
  }

  @Test
  fun `arrive maps correctly`() {
    assertEquals(Maneuver.TYPE_DESTINATION, ManeuverType.ARRIVE.toCarManeuverType(null))
  }

  @Test
  fun `continue maps to straight`() {
    assertEquals(Maneuver.TYPE_STRAIGHT, ManeuverType.CONTINUE.toCarManeuverType(null))
  }

  @Test
  fun `new name maps correctly`() {
    assertEquals(Maneuver.TYPE_NAME_CHANGE, ManeuverType.NEW_NAME.toCarManeuverType(null))
  }

  @Test
  fun `merge right maps correctly`() {
    assertEquals(
        Maneuver.TYPE_MERGE_RIGHT, ManeuverType.MERGE.toCarManeuverType(ManeuverModifier.RIGHT))
  }

  @Test
  fun `merge left maps correctly`() {
    assertEquals(
        Maneuver.TYPE_MERGE_LEFT, ManeuverType.MERGE.toCarManeuverType(ManeuverModifier.LEFT))
  }

  @Test
  fun `fork right maps correctly`() {
    assertEquals(
        Maneuver.TYPE_FORK_RIGHT, ManeuverType.FORK.toCarManeuverType(ManeuverModifier.RIGHT))
  }

  @Test
  fun `fork left maps correctly`() {
    assertEquals(
        Maneuver.TYPE_FORK_LEFT, ManeuverType.FORK.toCarManeuverType(ManeuverModifier.LEFT))
  }

  // Roundabout tests

  @Test
  fun `roundabout right-hand traffic is counterclockwise`() {
    assertEquals(
        Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW,
        ManeuverType.ROUNDABOUT.toCarManeuverType(null, DrivingSide.RIGHT))
  }

  @Test
  fun `roundabout left-hand traffic is clockwise`() {
    assertEquals(
        Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW,
        ManeuverType.ROUNDABOUT.toCarManeuverType(null, DrivingSide.LEFT))
  }

  @Test
  fun `exit roundabout right-hand traffic is counterclockwise`() {
    assertEquals(
        Maneuver.TYPE_ROUNDABOUT_EXIT_CCW,
        ManeuverType.EXIT_ROUNDABOUT.toCarManeuverType(null, DrivingSide.RIGHT))
  }

  @Test
  fun `exit roundabout left-hand traffic is clockwise`() {
    assertEquals(
        Maneuver.TYPE_ROUNDABOUT_EXIT_CW,
        ManeuverType.EXIT_ROUNDABOUT.toCarManeuverType(null, DrivingSide.LEFT))
  }

  @Test
  fun `rotary maps same as roundabout`() {
    assertEquals(
        Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW,
        ManeuverType.ROTARY.toCarManeuverType(null, DrivingSide.RIGHT))
  }

  // Null handling

  @Test
  fun `null maneuver type maps to unknown`() {
    assertEquals(Maneuver.TYPE_UNKNOWN, null.toCarManeuverType(null))
  }

  @Test
  fun `turn with null modifier maps to unknown`() {
    assertEquals(Maneuver.TYPE_UNKNOWN, ManeuverType.TURN.toCarManeuverType(null))
  }

  // isRoundaboutManeuverType

  @Test
  fun `roundabout types are detected`() {
    assertTrue(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW.isRoundaboutManeuverType())
    assertTrue(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW.isRoundaboutManeuverType())
    assertTrue(Maneuver.TYPE_ROUNDABOUT_ENTER_CW.isRoundaboutManeuverType())
    assertTrue(Maneuver.TYPE_ROUNDABOUT_ENTER_CCW.isRoundaboutManeuverType())
    assertTrue(Maneuver.TYPE_ROUNDABOUT_EXIT_CW.isRoundaboutManeuverType())
    assertTrue(Maneuver.TYPE_ROUNDABOUT_EXIT_CCW.isRoundaboutManeuverType())
  }

  @Test
  fun `non-roundabout types are not detected`() {
    assertFalse(Maneuver.TYPE_STRAIGHT.isRoundaboutManeuverType())
    assertFalse(Maneuver.TYPE_TURN_NORMAL_LEFT.isRoundaboutManeuverType())
    assertFalse(Maneuver.TYPE_DEPART.isRoundaboutManeuverType())
  }

  // Lane shape mapping

  @Test
  fun `lane shape straight`() {
    assertEquals(LaneDirection.SHAPE_STRAIGHT, "straight".toLaneShape())
  }

  @Test
  fun `lane shape slight left`() {
    assertEquals(LaneDirection.SHAPE_SLIGHT_LEFT, "slight left".toLaneShape())
    assertEquals(LaneDirection.SHAPE_SLIGHT_LEFT, "slightleft".toLaneShape())
  }

  @Test
  fun `lane shape slight right`() {
    assertEquals(LaneDirection.SHAPE_SLIGHT_RIGHT, "slight right".toLaneShape())
    assertEquals(LaneDirection.SHAPE_SLIGHT_RIGHT, "slightright".toLaneShape())
  }

  @Test
  fun `lane shape normal left and right`() {
    assertEquals(LaneDirection.SHAPE_NORMAL_LEFT, "left".toLaneShape())
    assertEquals(LaneDirection.SHAPE_NORMAL_RIGHT, "right".toLaneShape())
  }

  @Test
  fun `lane shape sharp left and right`() {
    assertEquals(LaneDirection.SHAPE_SHARP_LEFT, "sharp left".toLaneShape())
    assertEquals(LaneDirection.SHAPE_SHARP_RIGHT, "sharp right".toLaneShape())
  }

  @Test
  fun `lane shape uturn`() {
    assertEquals(LaneDirection.SHAPE_U_TURN_LEFT, "uturn".toLaneShape())
  }

  @Test
  fun `lane shape unknown for unrecognized`() {
    assertEquals(LaneDirection.SHAPE_UNKNOWN, "something else".toLaneShape())
  }

  @Test
  fun `lane shape is case insensitive`() {
    assertEquals(LaneDirection.SHAPE_STRAIGHT, "Straight".toLaneShape())
    assertEquals(LaneDirection.SHAPE_NORMAL_LEFT, "LEFT".toLaneShape())
  }
}
