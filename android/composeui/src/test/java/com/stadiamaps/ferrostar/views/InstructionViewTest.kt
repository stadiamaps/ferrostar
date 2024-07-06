package com.stadiamaps.ferrostar.views

import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5
import app.cash.paparazzi.Paparazzi
import com.stadiamaps.ferrostar.composeui.views.InstructionsView
import com.stadiamaps.ferrostar.support.withSnapshotBackground
import org.junit.Rule
import org.junit.Test
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.VisualInstruction
import uniffi.ferrostar.VisualInstructionContent

class InstructionViewTest {

  @get:Rule
  val paparazzi =
      Paparazzi(deviceConfig = PIXEL_5, theme = "android:Theme.Material.Light.NoActionBar")

  @Test
  fun testInstructionView() {
    val instructions =
        VisualInstruction(
            primaryContent =
                VisualInstructionContent(
                    text = "Hyde Street",
                    maneuverType = ManeuverType.TURN,
                    maneuverModifier = ManeuverModifier.LEFT,
                    roundaboutExitDegrees = null),
            secondaryContent = null,
            triggerDistanceBeforeManeuver = 42.0)

    paparazzi.snapshot {
      withSnapshotBackground {
        InstructionsView(instructions = instructions, distanceToNextManeuver = 42.0)
      }
    }
  }
}
