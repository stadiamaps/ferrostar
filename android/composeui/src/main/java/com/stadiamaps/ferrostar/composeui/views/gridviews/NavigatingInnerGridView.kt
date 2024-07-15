package com.stadiamaps.ferrostar.composeui.views.gridviews

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.views.controls.NavigationUIButton
import com.stadiamaps.ferrostar.composeui.views.controls.NavigationUIZoomButton

@Composable
fun NavigatingInnerGridView(
    modifier: Modifier,
    showZoom: Boolean = true,
    onClickZoomIn: () -> Unit = {},
    onClickZoomOut: () -> Unit = {},
    showCentering: Boolean = true,
    onClickCenter: () -> Unit = {},
    topCenter: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    topEnd: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    centerStart: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) },
    bottomEnd: @Composable () -> Unit = { Spacer(Modifier.width(12.dp)) }
) {
  InnerGridView(
      modifier,
      topStart = {
        // TODO: SpeedLimitView goes here
      },
      topCenter = topCenter,
      topEnd = topEnd,
      centerStart = centerStart,
      centerEnd = {
        if (showZoom) {
          NavigationUIZoomButton(onClickZoomIn, onClickZoomOut)
        }
      },
      bottomStart = {
        if (showCentering) {
          NavigationUIButton(onClick = onClickCenter) {
            Icon(Icons.Filled.Navigation, contentDescription = "Recenter Map")
          }
        }
      },
      bottomEnd = bottomEnd)
}

@Preview(device = Devices.PIXEL_5)
@Composable
fun NavigatingInnerGridViewPreview() {
  NavigatingInnerGridView(modifier = Modifier.fillMaxSize())
}

@Preview(
    device =
        "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
fun NavigatingInnerGridViewLandscapePreview() {
  NavigatingInnerGridView(modifier = Modifier.fillMaxSize())
}
