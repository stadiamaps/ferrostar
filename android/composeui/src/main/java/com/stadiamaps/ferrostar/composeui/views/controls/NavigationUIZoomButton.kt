package com.stadiamaps.ferrostar.composeui.views.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.R

@Composable
fun NavigationUIZoomButton(
    onClickZoomIn: () -> Unit,
    onClickZoomOut: () -> Unit,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor)
) {

  val elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)

  Column(modifier = Modifier.shadow(6.dp, shape = RoundedCornerShape(50))) {
    FloatingActionButton(
        onClick = onClickZoomIn,
        modifier = Modifier
          .height(56.dp)
          .width(56.dp),
        shape = RoundedCornerShape(topStartPercent = 50, topEndPercent = 50),
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation) {
          Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(id = R.string.zoom_in))
        }

    Box(modifier = Modifier
      .height(1.dp)
      .width(56.dp)) {
      HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }

    FloatingActionButton(
        onClick = onClickZoomOut,
        modifier = Modifier
          .height(56.dp)
          .width(56.dp),
        shape = RoundedCornerShape(bottomStartPercent = 50, bottomEndPercent = 50),
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation) {
          Icon(imageVector = Icons.Filled.Remove, contentDescription = stringResource(id = R.string.zoom_out))
        }
  }
}

@Preview
@Composable
fun NavigationUIZoomButtonPreview() {
  Box(
    Modifier
      .background(Color.LightGray)
      .padding(16.dp)) { NavigationUIZoomButton({}, {}) }
}
