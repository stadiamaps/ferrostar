package com.stadiamaps.ferrostar.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.views.controls.NavigationUIButton
import com.stadiamaps.ferrostar.composeui.views.controls.NavigationUIZoomButton
import com.stadiamaps.ferrostar.support.paparazziDefault
import org.junit.Rule
import org.junit.Test

class NavigationUIButtonTest {

  @get:Rule val paparazzi = paparazziDefault()

  @Test
  fun testNavigationUIButton() {
    paparazzi.snapshot {
      Box(modifier = Modifier.width(56.dp).height(56.dp).padding(16.dp)) {
        NavigationUIButton(onClick = { /* no action */ }) {
          Icon(Icons.Filled.Adb, contentDescription = "ADB")
        }
      }
    }
  }

  @Test
  fun testNavigationUIButtonCustomized() {
    paparazzi.snapshot {
      Box(modifier = Modifier.width(56.dp).height(56.dp).padding(16.dp)) {
        NavigationUIButton(
            onClick = { /* no action */ },
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
      Box(modifier = Modifier.width(56.dp).height(56.dp).padding(16.dp)) {
        NavigationUIZoomButton(
            onClickZoomIn = { /* no action */ }, onClickZoomOut = { /* no action */ })
      }
    }
  }

  @Test
  fun testNavigationUIZoomButtonCustomized() {
    paparazzi.snapshot {
      Box(modifier = Modifier.width(56.dp).height(56.dp).padding(16.dp)) {
        NavigationUIZoomButton(
            onClickZoomIn = { /* no action */ },
            onClickZoomOut = { /* no action */ },
            containerColor = Color.Black,
            contentColor = Color.White)
      }
    }
  }
}
