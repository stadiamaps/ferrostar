package com.stadiamaps.ferrostar.composeui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.VisualInstruction
import uniffi.ferrostar.VisualInstructionContent

val VisualInstructionContent.maneuverIcon: ImageVector?
  get() {
    // Stand-in art using Material Icons for now.
    // Ideally look for some iconography licensed under CC or similar
    // that we can use on all platforms.
    return when (this.maneuverModifier) {
      ManeuverModifier.U_TURN -> Icons.Filled.Info
      ManeuverModifier.SHARP_RIGHT -> Icons.Filled.Info
      ManeuverModifier.RIGHT -> Icons.Filled.Info
      ManeuverModifier.SLIGHT_RIGHT -> Icons.Filled.Info
      ManeuverModifier.STRAIGHT -> Icons.Filled.Info
      ManeuverModifier.SLIGHT_LEFT -> Icons.Filled.Info
      ManeuverModifier.LEFT -> Icons.Filled.Info
      ManeuverModifier.SHARP_LEFT -> Icons.Filled.Info
      else -> null
    }
  }

@Composable
fun BannerView(instructions: VisualInstruction, distanceToNextManeuver: Double?) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
              .padding(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                instructions.primaryContent.maneuverIcon?.let { icon ->
                  Image(
                      icon,
                      contentDescription = null,
                      modifier = Modifier.size(24.dp),
                      colorFilter = ColorFilter.tint(Color.White))
                }
                distanceToNextManeuver?.let {
                  // TODO: Format the text; Android appears to lack standard text styling!
                  Text(
                      text =
                          "${it.roundToInt()} m", // TODO: Replace with proper localized formatting
                      color = Color.White)
                }
              }
              // TODO: Text styling is placeholder
              Text(
                  text = instructions.primaryContent.text,
                  color = Color.White,
                  fontSize = 22.sp,
                  fontWeight = FontWeight.Bold)
            }
        instructions.secondaryContent?.let { Text(it.text) }
      }
}

@Preview
@Composable
fun PreviewBannerView() {
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

  BannerView(instructions = instructions, distanceToNextManeuver = 42.0)
}
