package com.stadiamaps.ferrostar.views

import com.stadiamaps.ferrostar.composeui.views.components.gridviews.InnerGridViewPreview
import com.stadiamaps.ferrostar.composeui.views.components.gridviews.InnerGridViewSampleLayoutPreview
import com.stadiamaps.ferrostar.support.paparazziDefault
import com.stadiamaps.ferrostar.support.withSnapshotBackground
import org.junit.Rule
import org.junit.Test

class InnerGridViewTest {

  @get:Rule val paparazzi = paparazziDefault()

  @Test
  fun testInnerGridViewAll() {
    paparazzi.snapshot { withSnapshotBackground { InnerGridViewPreview() } }
  }

  @Test
  fun testInnerGridViewSpecialized() {
    paparazzi.snapshot { withSnapshotBackground { InnerGridViewSampleLayoutPreview() } }
  }
}
