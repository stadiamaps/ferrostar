package com.stadiamaps.ferrostar.composeui.views

import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.formatting.DistanceFormatter
import com.stadiamaps.ferrostar.composeui.formatting.LocalizedDistanceFormatter
import com.stadiamaps.ferrostar.composeui.theme.DefaultInstructionRowTheme
import com.stadiamaps.ferrostar.composeui.theme.InstructionRowTheme
import com.stadiamaps.ferrostar.composeui.views.maneuver.ManeuverImage
import com.stadiamaps.ferrostar.composeui.views.maneuver.ManeuverInstructionView
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.VisualInstruction
import uniffi.ferrostar.VisualInstructionContent

/**
 * A banner view with sensible defaults.
 *
 * This banner view includes the default iconography from Mapbox, attempts to use the device's
 * locale for formatting distances and determining flow order (this can be overridden by passing a
 * customized formatter), and uses the material theme for color selection.
 */
@Composable
fun InstructionsView(
    instructions: VisualInstruction,
    distanceToNextManeuver: Double?,
    distanceFormatter: DistanceFormatter = LocalizedDistanceFormatter(),
    theme: InstructionRowTheme = DefaultInstructionRowTheme,
    content: @Composable () -> Unit = {
      ManeuverImage(instructions.primaryContent, tint = MaterialTheme.colorScheme.primary)
    }
) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .shadow(elevation = 5.dp, RoundedCornerShape(10.dp))
              .background(theme.backgroundColor, RoundedCornerShape(10.dp))
              .padding(8.dp)) {
        ManeuverInstructionView(
            text = instructions.primaryContent.text,
            distanceFormatter = distanceFormatter,
            distanceToNextManeuver = distanceToNextManeuver,
            theme = theme,
            content = content)
        // TODO: Secondary instructions
      }
}

// Previews

@Preview
@Composable
fun PreviewInstructionsView() {
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

  InstructionsView(instructions = instructions, distanceToNextManeuver = 42.0)
}

@Preview(locale = "ar")
@Composable
fun PreviewRTLInstructionsView() {
  val instructions =
      VisualInstruction(
          primaryContent =
              VisualInstructionContent(
                  text = "ادمج يسارًا",
                  maneuverType = ManeuverType.TURN,
                  maneuverModifier = ManeuverModifier.LEFT,
                  roundaboutExitDegrees = null,
                  laneInfo = null),
          secondaryContent = null,
          subContent = null,
          triggerDistanceBeforeManeuver = 42.0)

  InstructionsView(
      instructions = instructions,
      distanceFormatter = LocalizedDistanceFormatter(localeOverride = ULocale("ar")),
      distanceToNextManeuver = 42.0)
}
