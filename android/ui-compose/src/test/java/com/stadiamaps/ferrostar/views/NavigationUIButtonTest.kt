package com.stadiamaps.ferrostar.views

import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.views.components.controls.NavigationUIButton
import com.stadiamaps.ferrostar.composeui.views.components.controls.NavigationUIZoomButton
import com.stadiamaps.ferrostar.support.WithSnapshotBackground
import com.stadiamaps.ferrostar.support.paparazziDefault
import com.stadiamaps.ferrostar.ui.shared.R as SharedR
import org.junit.Rule
import org.junit.Test

class NavigationUIButtonTest {

  @get:Rule val paparazzi = paparazziDefault()

  private val buttonSize = DpSize(56.dp, 56.dp)

  @Test
  fun testNavigationUIButton() {
    paparazzi.snapshot {
      WithSnapshotBackground {
        NavigationUIButton(onClick = { /* no action */ }, buttonSize = buttonSize) {
          Icon(painterResource(SharedR.drawable.close_24px), contentDescription = "Close")
        }
      }
    }
  }

  @Test
  fun testNavigationUIButtonCustomized() {
    paparazzi.snapshot {
      WithSnapshotBackground {
        NavigationUIButton(
            onClick = { /* no action */ },
            buttonSize = buttonSize,
            containerColor = Color.Black,
            contentColor = Color.White,
        ) {
          Icon(painterResource(SharedR.drawable.close_24px), contentDescription = "Close")
        }
      }
    }
  }

  @Test
  fun testNavigationUIZoomButton() {
    paparazzi.snapshot {
      WithSnapshotBackground {
        NavigationUIZoomButton(
            onClickZoomIn = { /* no action */ },
            onClickZoomOut = { /* no action */ },
            buttonSize = buttonSize,
        )
      }
    }
  }

  @Test
  fun testNavigationUIZoomButtonCustomized() {
    paparazzi.snapshot {
      WithSnapshotBackground {
        NavigationUIZoomButton(
            buttonSize = buttonSize,
            onClickZoomIn = { /* no action */ },
            onClickZoomOut = { /* no action */ },
            containerColor = Color.Black,
            contentColor = Color.White,
        )
      }
    }
  }
}
