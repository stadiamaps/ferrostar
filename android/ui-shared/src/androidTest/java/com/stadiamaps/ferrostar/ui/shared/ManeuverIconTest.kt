package com.stadiamaps.ferrostar.ui.shared

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stadiamaps.ferrostar.ui.shared.icons.ManeuverIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType

@RunWith(AndroidJUnit4::class)
class ManeuverIconTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = InstrumentationRegistry.getInstrumentation().targetContext
  }

  @Test
  fun identifierFormat() {
    assertEquals(
        "direction_turn_left",
        ManeuverIcon(context, ManeuverType.TURN, ManeuverModifier.LEFT).identifier)
    assertEquals(
        "direction_new_name_sharp_right",
        ManeuverIcon(context, ManeuverType.NEW_NAME, ManeuverModifier.SHARP_RIGHT).identifier)
    assertEquals(
        "direction_continue_u_turn",
        ManeuverIcon(context, ManeuverType.CONTINUE, ManeuverModifier.U_TURN).identifier)
    assertEquals(
        "direction_end_of_road_left",
        ManeuverIcon(context, ManeuverType.END_OF_ROAD, ManeuverModifier.LEFT).identifier)
  }

  @Test
  fun knownDrawablesHaveNonNullResourceId() {
    val combinations =
        listOf(
            // Turn
            ManeuverType.TURN to ManeuverModifier.LEFT,
            ManeuverType.TURN to ManeuverModifier.SHARP_LEFT,
            ManeuverType.TURN to ManeuverModifier.SLIGHT_LEFT,
            ManeuverType.TURN to ManeuverModifier.RIGHT,
            ManeuverType.TURN to ManeuverModifier.SHARP_RIGHT,
            ManeuverType.TURN to ManeuverModifier.SLIGHT_RIGHT,
            ManeuverType.TURN to ManeuverModifier.STRAIGHT,
            // NewName
            ManeuverType.NEW_NAME to ManeuverModifier.LEFT,
            ManeuverType.NEW_NAME to ManeuverModifier.SHARP_LEFT,
            ManeuverType.NEW_NAME to ManeuverModifier.SLIGHT_LEFT,
            ManeuverType.NEW_NAME to ManeuverModifier.RIGHT,
            ManeuverType.NEW_NAME to ManeuverModifier.SHARP_RIGHT,
            ManeuverType.NEW_NAME to ManeuverModifier.SLIGHT_RIGHT,
            ManeuverType.NEW_NAME to ManeuverModifier.STRAIGHT,
            // Depart
            ManeuverType.DEPART to ManeuverModifier.LEFT,
            ManeuverType.DEPART to ManeuverModifier.RIGHT,
            ManeuverType.DEPART to ManeuverModifier.STRAIGHT,
            // Arrive
            ManeuverType.ARRIVE to ManeuverModifier.LEFT,
            ManeuverType.ARRIVE to ManeuverModifier.RIGHT,
            ManeuverType.ARRIVE to ManeuverModifier.STRAIGHT,
            // Merge
            ManeuverType.MERGE to ManeuverModifier.LEFT,
            ManeuverType.MERGE to ManeuverModifier.SLIGHT_LEFT,
            ManeuverType.MERGE to ManeuverModifier.RIGHT,
            ManeuverType.MERGE to ManeuverModifier.SLIGHT_RIGHT,
            ManeuverType.MERGE to ManeuverModifier.STRAIGHT,
            // OnRamp
            ManeuverType.ON_RAMP to ManeuverModifier.LEFT,
            ManeuverType.ON_RAMP to ManeuverModifier.SHARP_LEFT,
            ManeuverType.ON_RAMP to ManeuverModifier.SLIGHT_LEFT,
            ManeuverType.ON_RAMP to ManeuverModifier.RIGHT,
            ManeuverType.ON_RAMP to ManeuverModifier.SHARP_RIGHT,
            ManeuverType.ON_RAMP to ManeuverModifier.SLIGHT_RIGHT,
            ManeuverType.ON_RAMP to ManeuverModifier.STRAIGHT,
            // OffRamp
            ManeuverType.OFF_RAMP to ManeuverModifier.LEFT,
            ManeuverType.OFF_RAMP to ManeuverModifier.SLIGHT_LEFT,
            ManeuverType.OFF_RAMP to ManeuverModifier.RIGHT,
            ManeuverType.OFF_RAMP to ManeuverModifier.SLIGHT_RIGHT,
            // Fork
            ManeuverType.FORK to ManeuverModifier.LEFT,
            ManeuverType.FORK to ManeuverModifier.SLIGHT_LEFT,
            ManeuverType.FORK to ManeuverModifier.RIGHT,
            ManeuverType.FORK to ManeuverModifier.SLIGHT_RIGHT,
            ManeuverType.FORK to ManeuverModifier.STRAIGHT,
            // EndOfRoad
            ManeuverType.END_OF_ROAD to ManeuverModifier.LEFT,
            ManeuverType.END_OF_ROAD to ManeuverModifier.RIGHT,
            // Continue
            ManeuverType.CONTINUE to ManeuverModifier.LEFT,
            ManeuverType.CONTINUE to ManeuverModifier.SLIGHT_LEFT,
            ManeuverType.CONTINUE to ManeuverModifier.RIGHT,
            ManeuverType.CONTINUE to ManeuverModifier.SLIGHT_RIGHT,
            ManeuverType.CONTINUE to ManeuverModifier.STRAIGHT,
            ManeuverType.CONTINUE to ManeuverModifier.U_TURN,
            // Roundabout
            ManeuverType.ROUNDABOUT to ManeuverModifier.LEFT,
            ManeuverType.ROUNDABOUT to ManeuverModifier.SHARP_LEFT,
            ManeuverType.ROUNDABOUT to ManeuverModifier.SLIGHT_LEFT,
            ManeuverType.ROUNDABOUT to ManeuverModifier.RIGHT,
            ManeuverType.ROUNDABOUT to ManeuverModifier.SHARP_RIGHT,
            ManeuverType.ROUNDABOUT to ManeuverModifier.SLIGHT_RIGHT,
            ManeuverType.ROUNDABOUT to ManeuverModifier.STRAIGHT,
            // Rotary
            ManeuverType.ROTARY to ManeuverModifier.LEFT,
            ManeuverType.ROTARY to ManeuverModifier.SHARP_LEFT,
            ManeuverType.ROTARY to ManeuverModifier.SLIGHT_LEFT,
            ManeuverType.ROTARY to ManeuverModifier.RIGHT,
            ManeuverType.ROTARY to ManeuverModifier.SHARP_RIGHT,
            ManeuverType.ROTARY to ManeuverModifier.SLIGHT_RIGHT,
            ManeuverType.ROTARY to ManeuverModifier.STRAIGHT,
            // Notification
            ManeuverType.NOTIFICATION to ManeuverModifier.LEFT,
            ManeuverType.NOTIFICATION to ManeuverModifier.SHARP_LEFT,
            ManeuverType.NOTIFICATION to ManeuverModifier.SLIGHT_LEFT,
            ManeuverType.NOTIFICATION to ManeuverModifier.RIGHT,
            ManeuverType.NOTIFICATION to ManeuverModifier.SHARP_RIGHT,
            ManeuverType.NOTIFICATION to ManeuverModifier.SLIGHT_RIGHT,
            ManeuverType.NOTIFICATION to ManeuverModifier.STRAIGHT,
        )

    for ((type, modifier) in combinations) {
      val icon = ManeuverIcon(context, type, modifier)
      assertNotNull("Expected non-null resourceId for '${icon.identifier}'", icon.resourceId)
    }
  }

  @Test
  fun missingDrawableReturnsNullResourceId() {
    // No drawable exists for these type+modifier combinations
    assertNull(ManeuverIcon(context, ManeuverType.TURN, ManeuverModifier.U_TURN).resourceId)
    assertNull(
        ManeuverIcon(context, ManeuverType.ROUNDABOUT_TURN, ManeuverModifier.LEFT).resourceId)
    assertNull(
        ManeuverIcon(context, ManeuverType.EXIT_ROUNDABOUT, ManeuverModifier.LEFT).resourceId)
  }
}
