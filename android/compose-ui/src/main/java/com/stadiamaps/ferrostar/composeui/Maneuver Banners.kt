package com.stadiamaps.ferrostar.composeui

import android.annotation.SuppressLint
import android.icu.util.ULocale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.VisualInstruction
import uniffi.ferrostar.VisualInstructionContent

private val VisualInstructionContent.maneuverIcon: String
  get() {
    val descriptor =
        listOfNotNull(
                maneuverType?.name?.replace(" ", "_"), maneuverModifier?.name?.replace(" ", "_"))
            .joinToString(separator = "_")
    return "direction_${descriptor}".lowercase()
  }

/**
 * A generic maneuver instruction view.
 *
 * This is used as the building block for banner views. Formatting and styling are quite
 * customizable. The content composable closure renders the given view on the leading edge of the
 * view, and is most commonly an icon such as a turn arrow.
 */
@Composable
fun ManeuverInstructionView(
    text: String,
    distanceFormatter: DistanceFormatter,
    distanceToNextManeuver: Double?,
    theme: InstructionRowTheme = DefaultInstructionRowTheme,
    content: @Composable () -> Unit = {}
) {
  Row {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)) {
          content()
        }
    Column() {
      distanceToNextManeuver?.let {
        Text(
            text = distanceFormatter.format(distanceToNextManeuver),
            style = theme.distanceTextStyle)
      }
      Text(
          text = text,
          style = theme.instructionTextStyle,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis)
    }
  }
}

/** An icon view using the public domain drawables from Mapbox. */
@SuppressLint("DiscouragedApi")
@Composable
fun MapboxManeuverIcon(content: VisualInstructionContent, tint: Color = LocalContentColor.current) {
  val context = LocalContext.current
  val resourceId =
      context.resources.getIdentifier(content.maneuverIcon, "drawable", context.packageName)

  if (resourceId != 0) {
    Icon(
        painter = painterResource(id = resourceId),
        contentDescription = "Description for accessibility",
        tint = tint,
        modifier = Modifier.size(64.dp))
  } else {
    // Ignore resolution failures for the moment.
  }
}

/**
 * A banner view with sensible defaults.
 *
 * This banner view includes the default iconography from Mapbox, attempts to use the device's
 * locale for formatting distances and determining flow order (this can be overridden by passing a
 * customized formatter), and uses the material theme for color selection.
 */
@Composable
fun BannerInstructionView(
    instructions: VisualInstruction,
    distanceToNextManeuver: Double?,
    distanceFormatter: DistanceFormatter = LocalizedDistanceFormatter(),
    theme: InstructionRowTheme = DefaultInstructionRowTheme,
    content: @Composable () -> Unit = {
      MapboxManeuverIcon(instructions.primaryContent, tint = MaterialTheme.colorScheme.primary)
    }
) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .shadow(elevation = 5.dp, RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
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

  BannerInstructionView(instructions = instructions, distanceToNextManeuver = 42.0)
}

@Preview
@Composable
fun PreviewManeuverInstructionView() {
  ManeuverInstructionView(
      text = "Turn Right on Road Ave.",
      distanceFormatter = LocalizedDistanceFormatter(),
      distanceToNextManeuver = 24140.16)
}

@Preview
@Composable
fun PreviewImageManeuverInstructionView() {
  ManeuverInstructionView(
      text = "Turn Right on Road Ave.",
      distanceFormatter = LocalizedDistanceFormatter(),
      distanceToNextManeuver = 24140.16) {
        Image(
            Icons.Filled.Info,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(Color.White))
      }
}

@Preview(locale = "ar")
@Composable
fun PreviewRTLManeuverInstructionView() {
  ManeuverInstructionView(
      text = "ادمج يسارًا",
      distanceFormatter = LocalizedDistanceFormatter(localeOverride = ULocale("ar")),
      distanceToNextManeuver = 24140.16) {
        Image(
            Icons.Filled.Build,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(Color.White))
      }
}
