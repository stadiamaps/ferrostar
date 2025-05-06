package com.stadiamaps.ferrostar.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.views.components.controls.NavigationUIButton
import com.stadiamaps.ferrostar.composeui.views.components.controls.NavigationUIZoomButton
import com.stadiamaps.ferrostar.support.paparazziDefault
import org.junit.Rule
import org.junit.Test

class NavigationUIButtonTest {

  @get:Rule val paparazzi = paparazziDefault()

  private val buttonSize = DpSize(56.dp, 56.dp)

  @Test
  fun testNavigationUIButton() {
    paparazzi.snapshot {
      Box(modifier = Modifier.size(buttonSize).padding(16.dp)) {
        NavigationUIButton(onClick = { /* no action */ }, buttonSize = buttonSize) {
          Icon(Icons.Filled.Adb, contentDescription = "ADB")
        }
      }
    }
  }

  @Test
  fun testNavigationUIButtonCustomized() {
    paparazzi.snapshot {
      Box(modifier = Modifier.size(buttonSize).padding(16.dp)) {
        NavigationUIButton(
            onClick = { /* no action */ },
            buttonSize = buttonSize,
            containerColor = Color.Black,
            contentColor = Color.White) {
              Icon(Icons.Filled.Adb, contentDescription = "ADB")
            }
      }
    }
  }

  @Test
  fun testNavigationUIZoomButton() {
    paparazzi.snapshot {
      Box(modifier = Modifier.size(buttonSize).padding(16.dp)) {
        NavigationUIZoomButton(
            onClickZoomIn = { /* no action */ },
            onClickZoomOut = { /* no action */ },
            buttonSize = buttonSize)
      }
    }
  }

  @Test
  fun testNavigationUIZoomButtonCustomized() {
    paparazzi.snapshot {
      Box(modifier = Modifier.size(buttonSize).padding(16.dp)) {
        NavigationUIZoomButton(
            buttonSize = buttonSize,
            onClickZoomIn = { /* no action */ },
            onClickZoomOut = { /* no action */ },
            containerColor = Color.Black,
            contentColor = Color.White)
      }
    }
  }
}
