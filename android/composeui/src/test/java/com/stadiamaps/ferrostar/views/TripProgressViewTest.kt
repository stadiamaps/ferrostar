package com.stadiamaps.ferrostar.views

import com.stadiamaps.ferrostar.composeui.views.components.ProgressView24HourPreview
import com.stadiamaps.ferrostar.composeui.views.components.ProgressViewInformationalPreview
import com.stadiamaps.ferrostar.composeui.views.components.ProgressViewWithExitPreview
import com.stadiamaps.ferrostar.composeui.views.components.TripProgressView
import com.stadiamaps.ferrostar.support.paparazziDefault
import com.stadiamaps.ferrostar.support.withSnapshotBackground
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test
import uniffi.ferrostar.TripProgress

class TripProgressViewTest {

  @get:Rule val paparazzi = paparazziDefault()

  @OptIn(ExperimentalTime::class)
  @Test
  fun testProgressView() {
    paparazzi.snapshot {
      withSnapshotBackground {
        TripProgressView(
            progress =
                TripProgress(
                    distanceRemaining = 124252.0,
                    durationRemaining = 52012.0,
                    distanceToNextManeuver = 1257.0),
            fromDate = Instant.fromEpochSeconds(1720283624),
            timeZone = TimeZone.of("America/Los_Angeles"))
      }
    }
  }

  @Test
  fun testProgressViewInformationalStyle() {
    paparazzi.snapshot { withSnapshotBackground { ProgressViewInformationalPreview() } }
  }

  @Test
  fun testProgressViewWithExit() {
    paparazzi.snapshot { withSnapshotBackground { ProgressViewWithExitPreview() } }
  }

  @Test
  fun testProgressView24Hour() {
    paparazzi.snapshot { withSnapshotBackground { ProgressView24HourPreview() } }
  }
}
