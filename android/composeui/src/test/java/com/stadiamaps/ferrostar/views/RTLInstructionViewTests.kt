package com.stadiamaps.ferrostar.views

import android.icu.util.ULocale
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5
import app.cash.paparazzi.Paparazzi
import com.stadiamaps.ferrostar.composeui.formatting.LocalizedDistanceFormatter
import com.stadiamaps.ferrostar.composeui.views.components.InstructionsView
import com.stadiamaps.ferrostar.support.withSnapshotBackground
import org.junit.Rule
import org.junit.Test
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.VisualInstruction
import uniffi.ferrostar.VisualInstructionContent

class RTLInstructionViewTests {
  @get:Rule
  val paparazzi =
      Paparazzi(
          deviceConfig = PIXEL_5.copy(locale = "ar"),
          theme = "android:Theme.Material.Light.NoActionBar")

  @Test
  fun testRTLInstructionView() {
    val instructions =
        VisualInstruction(
            primaryContent =
                VisualInstructionContent(
                    text = "ادمج يسارًا",
                    maneuverType = ManeuverType.TURN,
                    maneuverModifier = ManeuverModifier.LEFT,
                    roundaboutExitDegrees = null,
                    laneInfo = null,
                    exitNumbers = emptyList()),
            secondaryContent = null,
            subContent = null,
            triggerDistanceBeforeManeuver = 42.0)

    paparazzi.snapshot {
      withSnapshotBackground {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          InstructionsView(
              instructions = instructions,
              distanceFormatter = LocalizedDistanceFormatter(localeOverride = ULocale("ar")),
              distanceToNextManeuver = 42.0)
        }
      }
    }
  }
}
