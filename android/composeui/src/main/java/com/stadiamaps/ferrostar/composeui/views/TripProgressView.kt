package com.stadiamaps.ferrostar.composeui.views

import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.R
import com.stadiamaps.ferrostar.composeui.formatting.DateTimeFormatter
import com.stadiamaps.ferrostar.composeui.formatting.DistanceFormatter
import com.stadiamaps.ferrostar.composeui.formatting.DurationFormatter
import com.stadiamaps.ferrostar.composeui.formatting.EstimatedArrivalDateTimeFormatter
import com.stadiamaps.ferrostar.composeui.formatting.LocalizedDistanceFormatter
import com.stadiamaps.ferrostar.composeui.formatting.LocalizedDurationFormatter
import com.stadiamaps.ferrostar.composeui.theme.DefaultTripProgressViewTheme
import com.stadiamaps.ferrostar.composeui.theme.TripProgressViewStyle
import com.stadiamaps.ferrostar.composeui.theme.TripProgressViewTheme
import com.stadiamaps.ferrostar.core.extensions.estimatedArrivalTime
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import uniffi.ferrostar.TripProgress

/**
 * A view that displays the estimated arrival time, the remaining distance, and the remaining
 * duration of a trip.
 *
 * @param modifier The modifier to apply to this layout.
 * @param theme The theme to use for this view.
 * @param estimatedArrivalFormatter The formatter to use for the estimated arrival time.
 * @param distanceFormatter The formatter to use for the distance.
 * @param durationFormatter The formatter to use for the duration.
 * @param progress The progress of the trip.
 * @param fromDate The date to use as the reference for the estimated arrival time.
 * @param timeZone The time zone used for the estimated arrival time formatter.
 * @param onTapExit An optional callback to invoke when the exit button is tapped. If null, the exit
 *   button is not displayed.
 */
@Composable
fun TripProgressView(
    modifier: Modifier = Modifier,
    theme: TripProgressViewTheme = DefaultTripProgressViewTheme,
    estimatedArrivalFormatter: DateTimeFormatter = EstimatedArrivalDateTimeFormatter(),
    distanceFormatter: DistanceFormatter = LocalizedDistanceFormatter(),
    durationFormatter: DurationFormatter = LocalizedDurationFormatter(),
    progress: TripProgress,
    fromDate: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    onTapExit: (() -> Unit)? = null
) {
  Box(modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Row(
          modifier =
              Modifier.shadow(12.dp, shape = RoundedCornerShape(50))
                  .background(color = theme.backgroundColor, shape = RoundedCornerShape(50))
                  .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                      text =
                          estimatedArrivalFormatter.format(
                              progress.estimatedArrivalTime(fromDate, timeZone)),
                      style = theme.measurementTextStyle)
                  if (theme.style == TripProgressViewStyle.INFORMATIONAL) {
                    Text(
                        text = stringResource(id = R.string.arrival),
                        style = theme.secondaryTextStyle)
                  }
                }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                      text = durationFormatter.format(progress.durationRemaining),
                      style = theme.measurementTextStyle)
                  if (theme.style == TripProgressViewStyle.INFORMATIONAL) {
                    Text(
                        text = stringResource(id = R.string.duration),
                        style = theme.secondaryTextStyle)
                  }
                }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                      text = distanceFormatter.format(progress.distanceRemaining),
                      style = theme.measurementTextStyle)
                  if (theme.style == TripProgressViewStyle.INFORMATIONAL) {
                    Text(
                        text = stringResource(id = R.string.distance),
                        style = theme.secondaryTextStyle)
                  }
                }

            // The optional exit button
            if (onTapExit != null) {
              Button(
                  onClick = { onTapExit() },
                  modifier = Modifier.size(50.dp),
                  shape = CircleShape,
                  colors =
                      ButtonDefaults.buttonColors(containerColor = theme.exitButtonBackgroundColor),
                  contentPadding = PaddingValues(0.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.end_navigation),
                        tint = theme.exitIconColor)
                  }
            }
          }
    }
  }
}

@Preview
@Composable
fun ProgressViewPreview() {
  TripProgressView(
      progress =
          TripProgress(
              distanceRemaining = 124252.0,
              durationRemaining = 52012.0,
              distanceToNextManeuver = 1257.0),
      fromDate = Instant.fromEpochSeconds(1720283624),
      timeZone = TimeZone.of("America/Los_Angeles"))
}

@Preview
@Composable
fun ProgressViewInformationalPreview() {
  val progress =
      TripProgress(
          distanceRemaining = 1000.0, durationRemaining = 1000.0, distanceToNextManeuver = 500.0)

  val theme =
      object : TripProgressViewTheme {
        override val style: TripProgressViewStyle
          @Composable get() = TripProgressViewStyle.INFORMATIONAL

        override val measurementTextStyle: TextStyle
          @Composable
          get() =
              MaterialTheme.typography.titleLarge.copy(
                  color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)

        override val secondaryTextStyle: TextStyle
          @Composable
          get() =
              MaterialTheme.typography.labelSmall.copy(
                  color = MaterialTheme.colorScheme.onBackground)

        override val exitIconColor: Color
          @Composable get() = MaterialTheme.colorScheme.onSecondary

        override val exitButtonBackgroundColor: Color
          @Composable get() = MaterialTheme.colorScheme.secondary

        override val backgroundColor: Color
          @Composable get() = MaterialTheme.colorScheme.background
      }

  TripProgressView(
      progress = progress,
      theme = theme,
      fromDate = Instant.fromEpochSeconds(1720283624),
      timeZone = TimeZone.of("America/Los_Angeles"))
}

@Preview
@Composable
fun ProgressViewWithExitPreview() {
  val progress =
      TripProgress(
          distanceRemaining = 2442522.0,
          durationRemaining = 52012.0,
          distanceToNextManeuver = 500.0)

  TripProgressView(
      progress = progress,
      fromDate = Instant.fromEpochSeconds(1720283624),
      timeZone = TimeZone.of("America/Los_Angeles"),
      onTapExit = { /* no-op */ })
}

@Preview(locale = "de_DE")
@Composable
fun ProgressView24HourPreview() {
  val estimatedArrivalFormatter =
      EstimatedArrivalDateTimeFormatter(localeOverride = ULocale.GERMANY)

  val progress =
      TripProgress(
          distanceRemaining = 2442522.0,
          durationRemaining = 52012.0,
          distanceToNextManeuver = 500.0)

  TripProgressView(
      progress = progress,
      estimatedArrivalFormatter = estimatedArrivalFormatter,
      fromDate = Instant.fromEpochSeconds(1720283624),
      timeZone = TimeZone.of("Europe/Berlin"),
      onTapExit = { /* no-op */ })
}
