package com.stadiamaps.ferrostar.views

import com.stadiamaps.ferrostar.composeui.views.gridviews.NavigatingInnerGridViewLandscapeNonTrackingPreview
import com.stadiamaps.ferrostar.composeui.views.gridviews.NavigatingInnerGridViewLandscapeTrackingPreview
import com.stadiamaps.ferrostar.composeui.views.gridviews.NavigatingInnerGridViewNonTrackingPreview
import com.stadiamaps.ferrostar.composeui.views.gridviews.NavigatingInnerGridViewTrackingPreview
import com.stadiamaps.ferrostar.support.paparazziDefault
import com.stadiamaps.ferrostar.support.withSnapshotBackground
import org.junit.Rule
import org.junit.Test

class NavigatingInnerGridViewTest {

  @get:Rule val paparazzi = paparazziDefault()

  @Test
  fun testNavigatingInnerGridViewTracking() {
    paparazzi.snapshot { withSnapshotBackground { NavigatingInnerGridViewTrackingPreview() } }
  }

  @Test
  fun testNavigatingInnerGridViewNonTracking() {
    paparazzi.snapshot { withSnapshotBackground { NavigatingInnerGridViewNonTrackingPreview() } }
  }

  @Test
  fun testNavigatingInnerGridViewTrackingLandscape() {
    paparazzi.snapshot {
      withSnapshotBackground { NavigatingInnerGridViewLandscapeTrackingPreview() }
    }
  }

  @Test
  fun testNavigatingInnerGridViewNonTrackingLandscape() {
    paparazzi.snapshot {
      withSnapshotBackground { NavigatingInnerGridViewLandscapeNonTrackingPreview() }
    }
  }
}
