package com.stadiamaps.ferrostar.views

import com.stadiamaps.ferrostar.composeui.views.components.gridviews.InnerGridViewPreview
import com.stadiamaps.ferrostar.composeui.views.components.gridviews.InnerGridViewSampleLayoutPreview
import com.stadiamaps.ferrostar.support.WithSnapshotBackground
import com.stadiamaps.ferrostar.support.paparazziDefault
import org.junit.Rule
import org.junit.Test

class InnerGridViewTest {

  @get:Rule val paparazzi = paparazziDefault()

  @Test
  fun testInnerGridViewAll() {
    paparazzi.snapshot { WithSnapshotBackground { InnerGridViewPreview() } }
  }

  @Test
  fun testInnerGridViewSpecialized() {
    paparazzi.snapshot { WithSnapshotBackground { InnerGridViewSampleLayoutPreview() } }
  }
}
