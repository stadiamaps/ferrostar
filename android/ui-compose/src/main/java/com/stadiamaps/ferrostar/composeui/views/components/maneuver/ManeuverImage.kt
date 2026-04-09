package com.stadiamaps.ferrostar.composeui.views.components.maneuver

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
import com.stadiamaps.ferrostar.ui.shared.icons.ManeuverIcon
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.VisualInstructionContent

/** An icon view using the public domain drawables from Mapbox. */
@Composable
fun ManeuverImage(content: VisualInstructionContent, tint: Color = LocalContentColor.current) {
  val maneuverType = content.maneuverType ?: return
  val maneuverModifier = content.maneuverModifier ?: return

  val context = LocalContext.current
  val maneuverIcon = ManeuverIcon(context, maneuverType, maneuverModifier)

  // Only display the icon if the resource was found.
  // Ignore resolution failures for the moment.
  maneuverIcon.resourceId?.let {
    Icon(
        painter = painterResource(id = it),
        contentDescription = stringResource(id = R.string.maneuver_image),
        tint = tint,
        modifier = Modifier.size(64.dp))
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
          laneInfo = null,
          exitNumbers = emptyList()))
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
          laneInfo = null,
          exitNumbers = emptyList()))
}
