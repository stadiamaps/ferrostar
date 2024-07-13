package com.stadiamaps.ferrostar.composeui.views.gridviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A General purpose grid view used for overlaying alerts, controls and other UI components on top
 * of the map's free space.
 *
 * | --- | --- | --- |
 * |-----|-----|-----|
 * | --- | --- | --- |
 *
 * @param topStart The composable to be placed in the top start position.
 * @param topCenter The composable to be placed in the top center position.
 * @param topEnd The composable to be placed in the top end position.
 * @param centerStart The composable to be placed in the center start position.
 * @param center The composable to be placed in the center position.
 * @param centerEnd The composable to be placed in the center end position.
 * @param bottomStart The composable to be placed in the bottom start position.
 * @param bottomCenter The composable to be placed in the bottom center position.
 * @param bottomEnd The composable to be placed in the bottom end position.
 */
@Composable
fun InnerGridView(
    modifier: Modifier,
    topStart: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    topCenter: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    topEnd: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    centerStart: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    center: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    centerEnd: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    bottomStart: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    bottomCenter: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    bottomEnd: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) }
) {
  // TODO: This
  Box(modifier) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
      Row(
          modifier = Modifier.fillMaxWidth().weight(1f),
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.SpaceBetween) {
            topStart()
            topCenter()
            topEnd()
          }
      Row(
          modifier = Modifier.fillMaxWidth().weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween) {
            centerStart()
            center()
            centerEnd()
          }
      Row(
          modifier = Modifier.fillMaxWidth().weight(1f),
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.SpaceBetween) {
            bottomStart()
            bottomCenter()
            bottomEnd()
          }
    }
  }
}

@Preview(device = Devices.PIXEL_5)
@Composable
fun InnerGridViewPreview() {
  Box(modifier = Modifier.background(color = Color.LightGray).padding(16.dp)) {
    InnerGridView(
        modifier = Modifier.fillMaxSize(),
        topStart = { SampleBox(size = 50.dp) },
        topCenter = { SampleBox(size = 50.dp) },
        topEnd = { SampleBox(size = 50.dp) },
        centerStart = { SampleBox(size = 50.dp) },
        center = { SampleBox(size = 50.dp) },
        centerEnd = { SampleBox(size = 150.dp) },
        bottomStart = { SampleBox(size = 50.dp) },
        bottomCenter = { SampleBox(size = 50.dp) },
        bottomEnd = { SampleBox(size = 50.dp) })
  }
}

@Preview(device = Devices.PIXEL_5)
@Composable
fun InnerGridViewSampleLayoutPreview() {
  Box(modifier = Modifier.background(color = Color.LightGray).padding(16.dp)) {
    InnerGridView(
        modifier = Modifier.fillMaxSize(),
        topStart = { SampleBox(size = 50.dp) },
        centerEnd = { SampleBox(size = 150.dp) },
        bottomStart = { SampleBox(size = 50.dp) })
  }
}

@Composable
private fun SampleBox(size: Dp) {
  Box(modifier = Modifier.height(size).width(size).background(color = Color.DarkGray))
}
