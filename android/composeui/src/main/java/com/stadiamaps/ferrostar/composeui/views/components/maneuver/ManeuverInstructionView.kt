package com.stadiamaps.ferrostar.composeui.views.components.maneuver

import android.icu.util.ULocale
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.formatting.DistanceFormatter
import com.stadiamaps.ferrostar.composeui.formatting.LocalizedDistanceFormatter
import com.stadiamaps.ferrostar.composeui.theme.DefaultInstructionRowTheme
import com.stadiamaps.ferrostar.composeui.theme.InstructionRowTheme

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
