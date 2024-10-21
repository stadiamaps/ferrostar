package com.stadiamaps.ferrostar.views

import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5
import app.cash.paparazzi.Paparazzi
import com.stadiamaps.ferrostar.composeui.views.maneuver.ManeuverImage
import org.junit.Rule
import org.junit.Test
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.VisualInstructionContent

class ManeuverImageTest {

  @get:Rule
  val paparazzi =
      Paparazzi(
          // Small size for maneuver images
          deviceConfig = PIXEL_5.copy(screenHeight = 180, screenWidth = 180),
          theme = "android:Theme.Material.Light.NoActionBar")

  @Test
  fun testManeuverImageTurnRight() {
    paparazzi.snapshot {
      ManeuverImage(
          VisualInstructionContent(
              text = "",
              maneuverType = ManeuverType.TURN,
              maneuverModifier = ManeuverModifier.RIGHT,
              roundaboutExitDegrees = null,
              laneInfo = null))
    }
  }

  @Test
  fun testManeuverImageForkLeft() {
    paparazzi.snapshot {
      ManeuverImage(
          VisualInstructionContent(
              text = "",
              maneuverType = ManeuverType.FORK,
              maneuverModifier = ManeuverModifier.LEFT,
              roundaboutExitDegrees = null,
              laneInfo = null))
    }
  }

  @Test
  fun testManeuverImageCustomColor() {
    paparazzi.snapshot {
      ManeuverImage(
          VisualInstructionContent(
              text = "",
              maneuverType = ManeuverType.FORK,
              maneuverModifier = ManeuverModifier.LEFT,
              roundaboutExitDegrees = null,
              laneInfo = null),
          tint = Color.Magenta)
    }
  }
}
