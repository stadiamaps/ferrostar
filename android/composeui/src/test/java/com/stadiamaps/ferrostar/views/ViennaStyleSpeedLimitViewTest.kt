package com.stadiamaps.ferrostar.views

import com.stadiamaps.ferrostar.composeui.views.components.speedlimit.ViennaConventionStyleSpeedLimitView
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeed
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeedUnit
import com.stadiamaps.ferrostar.support.paparazziDefault
import com.stadiamaps.ferrostar.support.withSnapshotBackground
import org.junit.Rule
import org.junit.Test

class ViennaStyleSpeedLimitViewTest {
  @get:Rule val paparazzi = paparazziDefault()

  @Test
  fun testLowSpeedValue() {
    paparazzi.snapshot {
      withSnapshotBackground {
        ViennaConventionStyleSpeedLimitView(
            speedLimit = MeasurementSpeed(55.0, MeasurementSpeedUnit.KilometersPerHour))
      }
    }
  }

  @Test
  fun testFastSpeedValue() {
    paparazzi.snapshot {
      withSnapshotBackground {
        ViennaConventionStyleSpeedLimitView(
            speedLimit = MeasurementSpeed(100.0, MeasurementSpeedUnit.KilometersPerHour))
      }
    }
  }

  @Test
  fun testImplausibleSpeedValue() {
    paparazzi.snapshot {
      withSnapshotBackground {
        ViennaConventionStyleSpeedLimitView(
            speedLimit = MeasurementSpeed(1000.0, MeasurementSpeedUnit.KilometersPerHour))
      }
    }
  }

  @Test
  fun testMetersPerSecondSpeedValue() {
    paparazzi.snapshot {
      withSnapshotBackground {
        ViennaConventionStyleSpeedLimitView(
            speedLimit = MeasurementSpeed(100.0, MeasurementSpeedUnit.MetersPerSecond),
            units = MeasurementSpeedUnit.MetersPerSecond)
      }
    }
  }

  @Test
  fun testMilesPerHourSpeedValue() {
    paparazzi.snapshot {
      withSnapshotBackground {
        ViennaConventionStyleSpeedLimitView(
            speedLimit = MeasurementSpeed(100.0, MeasurementSpeedUnit.MilesPerHour),
            units = MeasurementSpeedUnit.MilesPerHour)
      }
    }
  }

  @Test
  fun testKnotsSpeedValue() {
    paparazzi.snapshot {
      withSnapshotBackground {
        ViennaConventionStyleSpeedLimitView(
            speedLimit = MeasurementSpeed(100.0, MeasurementSpeedUnit.Knots),
            units = MeasurementSpeedUnit.Knots)
      }
    }
  }
}
