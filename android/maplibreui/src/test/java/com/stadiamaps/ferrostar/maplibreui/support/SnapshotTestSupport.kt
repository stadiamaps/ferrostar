package com.stadiamaps.ferrostar.maplibreui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import com.android.resources.ScreenOrientation

fun paparazziDefault(orientation: ScreenOrientation = ScreenOrientation.PORTRAIT): Paparazzi {
  return Paparazzi(
      deviceConfig =
          app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5.copy(orientation = orientation),
      theme = "android:Theme.Material.Light.NoActionBar",
      maxPercentDifference = 0.05)
}

/**
 * A composable that wraps the content in a box with a green background and padding. This provides a
 * contrasting and consistent background for snapshot tests.
 */
@Composable
fun WithSnapshotBackground(content: @Composable () -> Unit) {
  val greenBackground = Color(130, 203, 114)
  Box(modifier = Modifier.background(greenBackground).padding(16.dp)) { content() }
}
