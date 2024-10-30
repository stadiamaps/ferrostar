package com.stadiamaps.ferrostar.composeui.views

import android.icu.util.ULocale
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
import uniffi.ferrostar.RouteStep
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
    remainingSteps: List<RouteStep>? = null,
    initExpanded: Boolean = false,
    contentBuilder: @Composable (VisualInstruction) -> Unit = {
      ManeuverImage(it.primaryContent, tint = MaterialTheme.colorScheme.primary)
    }
) {
  var isExpanded by remember { mutableStateOf(initExpanded) }
  val screenHeight = LocalConfiguration.current.screenHeightDp
  val collapsedHeight = 100.dp

  val targetHeight = if (isExpanded) screenHeight.dp else collapsedHeight
  val animatedHeight by animateDpAsState(targetValue = targetHeight)

  val scrollState = rememberScrollState()

  Box(
      modifier =
          Modifier.fillMaxWidth()
              .height(animatedHeight)
              .background(theme.backgroundColor, RoundedCornerShape(10.dp))
              .clickable { isExpanded = !isExpanded }
              .padding(16.dp)) {
        Column(modifier = if (isExpanded) Modifier.verticalScroll(scrollState) else Modifier) {
          // Primary content
          ManeuverInstructionView(
              text = instructions.primaryContent.text,
              distanceFormatter = distanceFormatter,
              distanceToNextManeuver = distanceToNextManeuver,
              theme = theme) {
                contentBuilder(instructions)
              }

          // TODO: Secondary content

          // Expanded content
          if (isExpanded && remainingSteps != null && remainingSteps.count() > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp)
            remainingSteps.drop(1).forEach { step ->
              step.visualInstructions.firstOrNull()?.let { upcomingInstruction ->
                Spacer(modifier = Modifier.height(8.dp))
                ManeuverInstructionView(
                    text = upcomingInstruction.primaryContent.text,
                    distanceFormatter = distanceFormatter,
                    distanceToNextManeuver = step.distance,
                    theme = theme) {
                      contentBuilder(upcomingInstruction)
                    }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 1.dp)
              }
            }
          }
        }
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
