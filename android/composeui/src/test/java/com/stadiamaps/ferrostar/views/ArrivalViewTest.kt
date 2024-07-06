package com.stadiamaps.ferrostar.views

import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5
import app.cash.paparazzi.Paparazzi
import com.stadiamaps.ferrostar.composeui.views.ArrivalView
import com.stadiamaps.ferrostar.composeui.views.ArrivalView24HourPreview
import com.stadiamaps.ferrostar.composeui.views.ArrivalViewInformationalPreview
import com.stadiamaps.ferrostar.composeui.views.ArrivalViewWithExitPreview
import com.stadiamaps.ferrostar.support.withSnapshotBackground
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import uniffi.ferrostar.TripProgress

class ArrivalViewTest {

  @get:Rule
  val paparazzi =
      Paparazzi(
          deviceConfig = PIXEL_5.copy(),
          theme = "android:Theme.Material.Light.NoActionBar",
          maxPercentDifference = 0.01)

  @Test
  fun testArrivalView() {
    paparazzi.snapshot {
      withSnapshotBackground {
        ArrivalView(
            progress =
                TripProgress(
                    distanceRemaining = 124252.0,
                    durationRemaining = 52012.0,
                    distanceToNextManeuver = 1257.0),
            fromDate = Instant.fromEpochSeconds(1720283624))
      }
    }
  }

  @Test
  fun testArrivalViewInformationalStyle() {
    paparazzi.snapshot { withSnapshotBackground { ArrivalViewInformationalPreview() } }
  }

  @Test
  fun testArrivalViewWithExit() {
    paparazzi.snapshot { withSnapshotBackground { ArrivalViewWithExitPreview() } }
  }

  @Test
  fun testArrivalView24Hour() {
    paparazzi.snapshot { withSnapshotBackground { ArrivalView24HourPreview() } }
  }
}
