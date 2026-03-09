package com.stadiamaps.ferrostar.views

import com.stadiamaps.ferrostar.composeui.views.components.speedlimit.USStyleSpeedLimitView
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeed
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeedUnit
import com.stadiamaps.ferrostar.support.paparazziDefault
import com.stadiamaps.ferrostar.support.WithSnapshotBackground
import org.junit.Rule
import org.junit.Test

class USStyleSpeedLimitViewTest {
  @get:Rule val paparazzi = paparazziDefault()

  @Test
  fun testLowSpeedValue() {
    paparazzi.snapshot {
      WithSnapshotBackground {
        USStyleSpeedLimitView(
            speedLimit = MeasurementSpeed(55.0, MeasurementSpeedUnit.MilesPerHour))
      }
    }
  }

  @Test
  fun testFastSpeedValue() {
    paparazzi.snapshot {
      WithSnapshotBackground {
        USStyleSpeedLimitView(
            speedLimit = MeasurementSpeed(100.0, MeasurementSpeedUnit.MilesPerHour))
      }
    }
  }

  @Test
  fun testImplausibleSpeedValue() {
    paparazzi.snapshot {
      WithSnapshotBackground {
        USStyleSpeedLimitView(
            speedLimit = MeasurementSpeed(1000.0, MeasurementSpeedUnit.MilesPerHour))
      }
    }
  }

  @Test
  fun testKilometersPerHourSpeedValue() {
    paparazzi.snapshot {
      WithSnapshotBackground {
        USStyleSpeedLimitView(
            speedLimit = MeasurementSpeed(100.0, MeasurementSpeedUnit.KilometersPerHour),
            units = MeasurementSpeedUnit.KilometersPerHour)
      }
    }
  }

  @Test
  fun testKnotsSpeedValue() {
    paparazzi.snapshot {
      WithSnapshotBackground {
        USStyleSpeedLimitView(
            speedLimit = MeasurementSpeed(100.0, MeasurementSpeedUnit.Knots),
            units = MeasurementSpeedUnit.Knots)
      }
    }
  }
}
