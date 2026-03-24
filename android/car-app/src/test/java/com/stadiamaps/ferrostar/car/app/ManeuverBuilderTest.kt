package com.stadiamaps.ferrostar.car.app

import androidx.car.app.navigation.model.Maneuver
import com.stadiamaps.ferrostar.car.app.template.models.isRoundaboutManeuverType
import com.stadiamaps.ferrostar.car.app.template.models.toCarManeuverType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uniffi.ferrostar.DrivingSide
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType

class ManeuverBuilderTest {

  // --- Null / catch-all types ---

  @Test
  fun `null type returns UNKNOWN`() {
    assertEquals(Maneuver.TYPE_UNKNOWN, null.toCarManeuverType(null))
  }

  @Test
  fun `NOTIFICATION returns UNKNOWN`() {
    assertEquals(Maneuver.TYPE_UNKNOWN, ManeuverType.NOTIFICATION.toCarManeuverType(null))
  }

  // --- Simple single-value types ---

  @Test
  fun `NEW_NAME returns NAME_CHANGE`() {
    assertEquals(Maneuver.TYPE_NAME_CHANGE, ManeuverType.NEW_NAME.toCarManeuverType(null))
  }

  @Test
  fun `DEPART returns DEPART`() {
    assertEquals(Maneuver.TYPE_DEPART, ManeuverType.DEPART.toCarManeuverType(null))
  }

  @Test
  fun `ARRIVE returns DESTINATION`() {
    assertEquals(Maneuver.TYPE_DESTINATION, ManeuverType.ARRIVE.toCarManeuverType(null))
  }

  @Test
  fun `CONTINUE returns STRAIGHT`() {
    assertEquals(Maneuver.TYPE_STRAIGHT, ManeuverType.CONTINUE.toCarManeuverType(null))
  }

  // --- TURN ---

  @Test
  fun `TURN U_TURN returns U_TURN_LEFT`() {
    assertEquals(Maneuver.TYPE_U_TURN_LEFT, ManeuverType.TURN.toCarManeuverType(ManeuverModifier.U_TURN))
  }

  @Test
  fun `TURN SHARP_RIGHT returns TURN_SHARP_RIGHT`() {
    assertEquals(Maneuver.TYPE_TURN_SHARP_RIGHT, ManeuverType.TURN.toCarManeuverType(ManeuverModifier.SHARP_RIGHT))
  }

  @Test
  fun `TURN RIGHT returns TURN_NORMAL_RIGHT`() {
    assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, ManeuverType.TURN.toCarManeuverType(ManeuverModifier.RIGHT))
  }

  @Test
  fun `TURN SLIGHT_RIGHT returns TURN_SLIGHT_RIGHT`() {
    assertEquals(Maneuver.TYPE_TURN_SLIGHT_RIGHT, ManeuverType.TURN.toCarManeuverType(ManeuverModifier.SLIGHT_RIGHT))
  }

  @Test
  fun `TURN STRAIGHT returns STRAIGHT`() {
    assertEquals(Maneuver.TYPE_STRAIGHT, ManeuverType.TURN.toCarManeuverType(ManeuverModifier.STRAIGHT))
  }

  @Test
  fun `TURN SLIGHT_LEFT returns TURN_SLIGHT_LEFT`() {
    assertEquals(Maneuver.TYPE_TURN_SLIGHT_LEFT, ManeuverType.TURN.toCarManeuverType(ManeuverModifier.SLIGHT_LEFT))
  }

  @Test
  fun `TURN LEFT returns TURN_NORMAL_LEFT`() {
    assertEquals(Maneuver.TYPE_TURN_NORMAL_LEFT, ManeuverType.TURN.toCarManeuverType(ManeuverModifier.LEFT))
  }

  @Test
  fun `TURN SHARP_LEFT returns TURN_SHARP_LEFT`() {
    assertEquals(Maneuver.TYPE_TURN_SHARP_LEFT, ManeuverType.TURN.toCarManeuverType(ManeuverModifier.SHARP_LEFT))
  }

  @Test
  fun `TURN null modifier returns UNKNOWN`() {
    assertEquals(Maneuver.TYPE_UNKNOWN, ManeuverType.TURN.toCarManeuverType(null))
  }

  // --- MERGE ---

  @Test
  fun `MERGE SLIGHT_RIGHT returns MERGE_RIGHT`() {
    assertEquals(Maneuver.TYPE_MERGE_RIGHT, ManeuverType.MERGE.toCarManeuverType(ManeuverModifier.SLIGHT_RIGHT))
  }

  @Test
  fun `MERGE RIGHT returns MERGE_RIGHT`() {
    assertEquals(Maneuver.TYPE_MERGE_RIGHT, ManeuverType.MERGE.toCarManeuverType(ManeuverModifier.RIGHT))
  }

  @Test
  fun `MERGE SHARP_RIGHT returns MERGE_RIGHT`() {
    assertEquals(Maneuver.TYPE_MERGE_RIGHT, ManeuverType.MERGE.toCarManeuverType(ManeuverModifier.SHARP_RIGHT))
  }

  @Test
  fun `MERGE SLIGHT_LEFT returns MERGE_LEFT`() {
    assertEquals(Maneuver.TYPE_MERGE_LEFT, ManeuverType.MERGE.toCarManeuverType(ManeuverModifier.SLIGHT_LEFT))
  }

  @Test
  fun `MERGE LEFT returns MERGE_LEFT`() {
    assertEquals(Maneuver.TYPE_MERGE_LEFT, ManeuverType.MERGE.toCarManeuverType(ManeuverModifier.LEFT))
  }

  @Test
  fun `MERGE SHARP_LEFT returns MERGE_LEFT`() {
    assertEquals(Maneuver.TYPE_MERGE_LEFT, ManeuverType.MERGE.toCarManeuverType(ManeuverModifier.SHARP_LEFT))
  }

  @Test
  fun `MERGE STRAIGHT returns MERGE_SIDE_UNSPECIFIED`() {
    assertEquals(Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED, ManeuverType.MERGE.toCarManeuverType(ManeuverModifier.STRAIGHT))
  }

  @Test
  fun `MERGE null modifier returns MERGE_SIDE_UNSPECIFIED`() {
    assertEquals(Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED, ManeuverType.MERGE.toCarManeuverType(null))
  }

  // --- ON_RAMP ---

  @Test
  fun `ON_RAMP SLIGHT_RIGHT returns ON_RAMP_NORMAL_RIGHT`() {
    assertEquals(Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT, ManeuverType.ON_RAMP.toCarManeuverType(ManeuverModifier.SLIGHT_RIGHT))
  }

  @Test
  fun `ON_RAMP RIGHT returns ON_RAMP_NORMAL_RIGHT`() {
    assertEquals(Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT, ManeuverType.ON_RAMP.toCarManeuverType(ManeuverModifier.RIGHT))
  }

  @Test
  fun `ON_RAMP SHARP_RIGHT returns ON_RAMP_NORMAL_RIGHT`() {
    assertEquals(Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT, ManeuverType.ON_RAMP.toCarManeuverType(ManeuverModifier.SHARP_RIGHT))
  }

  @Test
  fun `ON_RAMP SLIGHT_LEFT returns ON_RAMP_NORMAL_LEFT`() {
    assertEquals(Maneuver.TYPE_ON_RAMP_NORMAL_LEFT, ManeuverType.ON_RAMP.toCarManeuverType(ManeuverModifier.SLIGHT_LEFT))
  }

  @Test
  fun `ON_RAMP LEFT returns ON_RAMP_NORMAL_LEFT`() {
    assertEquals(Maneuver.TYPE_ON_RAMP_NORMAL_LEFT, ManeuverType.ON_RAMP.toCarManeuverType(ManeuverModifier.LEFT))
  }

  @Test
  fun `ON_RAMP SHARP_LEFT returns ON_RAMP_NORMAL_LEFT`() {
    assertEquals(Maneuver.TYPE_ON_RAMP_NORMAL_LEFT, ManeuverType.ON_RAMP.toCarManeuverType(ManeuverModifier.SHARP_LEFT))
  }

  @Test
  fun `ON_RAMP null modifier defaults to ON_RAMP_NORMAL_RIGHT`() {
    assertEquals(Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT, ManeuverType.ON_RAMP.toCarManeuverType(null))
  }

  // --- OFF_RAMP ---

  @Test
  fun `OFF_RAMP SLIGHT_RIGHT returns OFF_RAMP_NORMAL_RIGHT`() {
    assertEquals(Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT, ManeuverType.OFF_RAMP.toCarManeuverType(ManeuverModifier.SLIGHT_RIGHT))
  }

  @Test
  fun `OFF_RAMP RIGHT returns OFF_RAMP_NORMAL_RIGHT`() {
    assertEquals(Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT, ManeuverType.OFF_RAMP.toCarManeuverType(ManeuverModifier.RIGHT))
  }

  @Test
  fun `OFF_RAMP SHARP_RIGHT returns OFF_RAMP_NORMAL_RIGHT`() {
    assertEquals(Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT, ManeuverType.OFF_RAMP.toCarManeuverType(ManeuverModifier.SHARP_RIGHT))
  }

  @Test
  fun `OFF_RAMP SLIGHT_LEFT returns OFF_RAMP_NORMAL_LEFT`() {
    assertEquals(Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT, ManeuverType.OFF_RAMP.toCarManeuverType(ManeuverModifier.SLIGHT_LEFT))
  }

  @Test
  fun `OFF_RAMP LEFT returns OFF_RAMP_NORMAL_LEFT`() {
    assertEquals(Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT, ManeuverType.OFF_RAMP.toCarManeuverType(ManeuverModifier.LEFT))
  }

  @Test
  fun `OFF_RAMP SHARP_LEFT returns OFF_RAMP_NORMAL_LEFT`() {
    assertEquals(Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT, ManeuverType.OFF_RAMP.toCarManeuverType(ManeuverModifier.SHARP_LEFT))
  }

  @Test
  fun `OFF_RAMP null modifier defaults to OFF_RAMP_NORMAL_RIGHT`() {
    assertEquals(Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT, ManeuverType.OFF_RAMP.toCarManeuverType(null))
  }

  // --- FORK ---

  @Test
  fun `FORK SLIGHT_RIGHT returns FORK_RIGHT`() {
    assertEquals(Maneuver.TYPE_FORK_RIGHT, ManeuverType.FORK.toCarManeuverType(ManeuverModifier.SLIGHT_RIGHT))
  }

  @Test
  fun `FORK RIGHT returns FORK_RIGHT`() {
    assertEquals(Maneuver.TYPE_FORK_RIGHT, ManeuverType.FORK.toCarManeuverType(ManeuverModifier.RIGHT))
  }

  @Test
  fun `FORK SHARP_RIGHT returns FORK_RIGHT`() {
    assertEquals(Maneuver.TYPE_FORK_RIGHT, ManeuverType.FORK.toCarManeuverType(ManeuverModifier.SHARP_RIGHT))
  }

  @Test
  fun `FORK SLIGHT_LEFT returns FORK_LEFT`() {
    assertEquals(Maneuver.TYPE_FORK_LEFT, ManeuverType.FORK.toCarManeuverType(ManeuverModifier.SLIGHT_LEFT))
  }

  @Test
  fun `FORK LEFT returns FORK_LEFT`() {
    assertEquals(Maneuver.TYPE_FORK_LEFT, ManeuverType.FORK.toCarManeuverType(ManeuverModifier.LEFT))
  }

  @Test
  fun `FORK SHARP_LEFT returns FORK_LEFT`() {
    assertEquals(Maneuver.TYPE_FORK_LEFT, ManeuverType.FORK.toCarManeuverType(ManeuverModifier.SHARP_LEFT))
  }

  @Test
  fun `FORK null modifier defaults to FORK_RIGHT`() {
    assertEquals(Maneuver.TYPE_FORK_RIGHT, ManeuverType.FORK.toCarManeuverType(null))
  }

  // --- END_OF_ROAD ---

  @Test
  fun `END_OF_ROAD RIGHT returns TURN_NORMAL_RIGHT`() {
    assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, ManeuverType.END_OF_ROAD.toCarManeuverType(ManeuverModifier.RIGHT))
  }

  @Test
  fun `END_OF_ROAD SLIGHT_RIGHT returns TURN_NORMAL_RIGHT`() {
    assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, ManeuverType.END_OF_ROAD.toCarManeuverType(ManeuverModifier.SLIGHT_RIGHT))
  }

  @Test
  fun `END_OF_ROAD SHARP_RIGHT returns TURN_NORMAL_RIGHT`() {
    assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, ManeuverType.END_OF_ROAD.toCarManeuverType(ManeuverModifier.SHARP_RIGHT))
  }

  @Test
  fun `END_OF_ROAD LEFT returns TURN_NORMAL_LEFT`() {
    assertEquals(Maneuver.TYPE_TURN_NORMAL_LEFT, ManeuverType.END_OF_ROAD.toCarManeuverType(ManeuverModifier.LEFT))
  }

  @Test
  fun `END_OF_ROAD SLIGHT_LEFT returns TURN_NORMAL_LEFT`() {
    assertEquals(Maneuver.TYPE_TURN_NORMAL_LEFT, ManeuverType.END_OF_ROAD.toCarManeuverType(ManeuverModifier.SLIGHT_LEFT))
  }

  @Test
  fun `END_OF_ROAD SHARP_LEFT returns TURN_NORMAL_LEFT`() {
    assertEquals(Maneuver.TYPE_TURN_NORMAL_LEFT, ManeuverType.END_OF_ROAD.toCarManeuverType(ManeuverModifier.SHARP_LEFT))
  }

  @Test
  fun `END_OF_ROAD STRAIGHT returns UNKNOWN`() {
    assertEquals(Maneuver.TYPE_UNKNOWN, ManeuverType.END_OF_ROAD.toCarManeuverType(ManeuverModifier.STRAIGHT))
  }

  @Test
  fun `END_OF_ROAD null modifier returns UNKNOWN`() {
    assertEquals(Maneuver.TYPE_UNKNOWN, ManeuverType.END_OF_ROAD.toCarManeuverType(null))
  }

  // --- Roundabout / Rotary — driving side determines CW vs CCW ---

  @Test
  fun `ROUNDABOUT right-hand traffic enters CCW`() {
    assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW,
        ManeuverType.ROUNDABOUT.toCarManeuverType(null, DrivingSide.RIGHT))
  }

  @Test
  fun `ROUNDABOUT left-hand traffic enters CW`() {
    assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW,
        ManeuverType.ROUNDABOUT.toCarManeuverType(null, DrivingSide.LEFT))
  }

  @Test
  fun `ROTARY right-hand traffic enters CCW`() {
    assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW,
        ManeuverType.ROTARY.toCarManeuverType(null, DrivingSide.RIGHT))
  }

  @Test
  fun `ROTARY left-hand traffic enters CW`() {
    assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW,
        ManeuverType.ROTARY.toCarManeuverType(null, DrivingSide.LEFT))
  }

  @Test
  fun `ROUNDABOUT_TURN right-hand traffic enters CCW`() {
    assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW,
        ManeuverType.ROUNDABOUT_TURN.toCarManeuverType(null, DrivingSide.RIGHT))
  }

  @Test
  fun `ROUNDABOUT_TURN left-hand traffic enters CW`() {
    assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW,
        ManeuverType.ROUNDABOUT_TURN.toCarManeuverType(null, DrivingSide.LEFT))
  }

  @Test
  fun `EXIT_ROUNDABOUT right-hand traffic exits CCW`() {
    assertEquals(Maneuver.TYPE_ROUNDABOUT_EXIT_CCW,
        ManeuverType.EXIT_ROUNDABOUT.toCarManeuverType(null, DrivingSide.RIGHT))
  }

  @Test
  fun `EXIT_ROUNDABOUT left-hand traffic exits CW`() {
    assertEquals(Maneuver.TYPE_ROUNDABOUT_EXIT_CW,
        ManeuverType.EXIT_ROUNDABOUT.toCarManeuverType(null, DrivingSide.LEFT))
  }

  @Test
  fun `EXIT_ROTARY right-hand traffic exits CCW`() {
    assertEquals(Maneuver.TYPE_ROUNDABOUT_EXIT_CCW,
        ManeuverType.EXIT_ROTARY.toCarManeuverType(null, DrivingSide.RIGHT))
  }

  @Test
  fun `EXIT_ROTARY left-hand traffic exits CW`() {
    assertEquals(Maneuver.TYPE_ROUNDABOUT_EXIT_CW,
        ManeuverType.EXIT_ROTARY.toCarManeuverType(null, DrivingSide.LEFT))
  }

  // --- isRoundaboutManeuverType ---

  @Test
  fun `roundabout enter-and-exit constants are identified as roundabout types`() {
    assertTrue(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW.isRoundaboutManeuverType())
    assertTrue(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW.isRoundaboutManeuverType())
  }

  @Test
  fun `roundabout enter constants are identified as roundabout types`() {
    assertTrue(Maneuver.TYPE_ROUNDABOUT_ENTER_CW.isRoundaboutManeuverType())
    assertTrue(Maneuver.TYPE_ROUNDABOUT_ENTER_CCW.isRoundaboutManeuverType())
  }

  @Test
  fun `roundabout exit constants are identified as roundabout types`() {
    assertTrue(Maneuver.TYPE_ROUNDABOUT_EXIT_CW.isRoundaboutManeuverType())
    assertTrue(Maneuver.TYPE_ROUNDABOUT_EXIT_CCW.isRoundaboutManeuverType())
  }

  @Test
  fun `non-roundabout constants are not identified as roundabout types`() {
    assertFalse(Maneuver.TYPE_STRAIGHT.isRoundaboutManeuverType())
    assertFalse(Maneuver.TYPE_UNKNOWN.isRoundaboutManeuverType())
    assertFalse(Maneuver.TYPE_DEPART.isRoundaboutManeuverType())
    assertFalse(Maneuver.TYPE_DESTINATION.isRoundaboutManeuverType())
    assertFalse(Maneuver.TYPE_TURN_NORMAL_RIGHT.isRoundaboutManeuverType())
    assertFalse(Maneuver.TYPE_TURN_NORMAL_LEFT.isRoundaboutManeuverType())
    assertFalse(Maneuver.TYPE_FORK_RIGHT.isRoundaboutManeuverType())
    assertFalse(Maneuver.TYPE_MERGE_RIGHT.isRoundaboutManeuverType())
  }
}
