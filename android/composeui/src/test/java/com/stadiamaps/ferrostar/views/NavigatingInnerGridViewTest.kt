package com.stadiamaps.ferrostar.views

import com.stadiamaps.ferrostar.composeui.views.components.gridviews.NavigatingInnerGridViewLandscapeNonTrackingPreview
import com.stadiamaps.ferrostar.composeui.views.components.gridviews.NavigatingInnerGridViewLandscapeTrackingPreview
import com.stadiamaps.ferrostar.composeui.views.components.gridviews.NavigatingInnerGridViewNonTrackingPreview
import com.stadiamaps.ferrostar.composeui.views.components.gridviews.NavigatingInnerGridViewTrackingPreview
import com.stadiamaps.ferrostar.support.WithSnapshotBackground
import com.stadiamaps.ferrostar.support.paparazziDefault
import org.junit.Rule
import org.junit.Test

class NavigatingInnerGridViewTest {

  @get:Rule val paparazzi = paparazziDefault()

  @Test
  fun testNavigatingInnerGridViewTracking() {
    paparazzi.snapshot { WithSnapshotBackground { NavigatingInnerGridViewTrackingPreview() } }
  }

  @Test
  fun testNavigatingInnerGridViewNonTracking() {
    paparazzi.snapshot { WithSnapshotBackground { NavigatingInnerGridViewNonTrackingPreview() } }
  }

  @Test
  fun testNavigatingInnerGridViewTrackingLandscape() {
    paparazzi.snapshot {
      WithSnapshotBackground { NavigatingInnerGridViewLandscapeTrackingPreview() }
    }
  }

  @Test
  fun testNavigatingInnerGridViewNonTrackingLandscape() {
    paparazzi.snapshot {
      WithSnapshotBackground { NavigatingInnerGridViewLandscapeNonTrackingPreview() }
    }
  }
}
