package com.stadiamaps.ferrostar.composeui.views.maneuver

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.R
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.VisualInstructionContent

val VisualInstructionContent.maneuverIcon: String
  get() {
    val descriptor =
        listOfNotNull(
                maneuverType?.name?.replace(" ", "_"), maneuverModifier?.name?.replace(" ", "_"))
            .joinToString(separator = "_")
    return "direction_${descriptor}".lowercase()
  }

/** An icon view using the public domain drawables from Mapbox. */
@SuppressLint("DiscouragedApi")
@Composable
fun ManeuverImage(content: VisualInstructionContent, tint: Color = LocalContentColor.current) {
  val context = LocalContext.current
  val resourceId =
      context.resources.getIdentifier(content.maneuverIcon, "drawable", context.packageName)

  if (resourceId != 0) {
    Icon(
        painter = painterResource(id = resourceId),
        contentDescription = stringResource(id = R.string.maneuver_image),
        tint = tint,
        modifier = Modifier.size(64.dp))
  } else {
    // Ignore resolution failures for the moment.
  }
}

@Preview
@Composable
fun ManeuverImageLeftTurnPreview() {
  ManeuverImage(
      VisualInstructionContent(
          text = "",
          maneuverType = ManeuverType.TURN,
          maneuverModifier = ManeuverModifier.LEFT,
          roundaboutExitDegrees = null,
          laneInfo = null))
}

@Preview
@Composable
fun ManeuverImageContinueUturnPreview() {
  ManeuverImage(
      VisualInstructionContent(
          text = "",
          maneuverType = ManeuverType.CONTINUE,
          maneuverModifier = ManeuverModifier.U_TURN,
          roundaboutExitDegrees = null,
          laneInfo = null))
}
