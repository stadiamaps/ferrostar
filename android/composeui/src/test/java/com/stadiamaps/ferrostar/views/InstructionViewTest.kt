package com.stadiamaps.ferrostar.views

import com.stadiamaps.ferrostar.composeui.views.InstructionsView
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.mock.pedestrianExample
import com.stadiamaps.ferrostar.support.paparazziDefault
import com.stadiamaps.ferrostar.support.withSnapshotBackground
import org.junit.Rule
import org.junit.Test
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.VisualInstruction
import uniffi.ferrostar.VisualInstructionContent

class InstructionViewTest {

  @get:Rule val paparazzi = paparazziDefault()

  @Test
  fun testInstructionView() {
    val instructions =
        VisualInstruction(
            primaryContent =
                VisualInstructionContent(
                    text = "Hyde Street",
                    maneuverType = ManeuverType.TURN,
                    maneuverModifier = ManeuverModifier.LEFT,
                    roundaboutExitDegrees = null,
                    laneInfo = null),
            secondaryContent = null,
            subContent = null,
            triggerDistanceBeforeManeuver = 42.0)

    paparazzi.snapshot {
      withSnapshotBackground {
        InstructionsView(instructions = instructions, distanceToNextManeuver = 42.0)
      }
    }
  }

  @Test
  fun testInstructionViewExpanded() {
    val state = NavigationUiState.pedestrianExample()

    paparazzi.snapshot {
      withSnapshotBackground {
        InstructionsView(
            instructions = state.visualInstruction!!,
            remainingSteps = state.remainingSteps,
            distanceToNextManeuver = 42.0,
            initExpanded = true)
      }
    }
  }
}
