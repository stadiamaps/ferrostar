package com.stadiamaps.ferrostar.views

import com.stadiamaps.ferrostar.composeui.views.gridviews.NavigatingInnerGridViewLandscapePreview
import com.stadiamaps.ferrostar.composeui.views.gridviews.NavigatingInnerGridViewPreview
import com.stadiamaps.ferrostar.support.paparazziDefault
import com.stadiamaps.ferrostar.support.withSnapshotBackground
import org.junit.Rule
import org.junit.Test

class NavigatingInnerGridViewTest {

  @get:Rule val paparazzi = paparazziDefault()

  @Test
  fun testNavigatingInnerGridView() {
    paparazzi.snapshot { withSnapshotBackground { NavigatingInnerGridViewPreview() } }
  }

  @Test
  fun testNavigatingInnerGridViewLandscape() {
    paparazzi.snapshot { withSnapshotBackground { NavigatingInnerGridViewLandscapePreview() } }
  }
}
