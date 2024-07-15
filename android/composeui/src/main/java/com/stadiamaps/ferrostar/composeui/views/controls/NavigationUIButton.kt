package com.stadiamaps.ferrostar.composeui.views.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A FloatingActionButton styled for use in the navigation UI.
 *
 * @param onClick
 * @param containerColor
 * @param contentColor
 * @param content
 */
@Composable
fun NavigationUIButton(
    onClick: () -> Unit,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable () -> Unit
) {
  FloatingActionButton(
      onClick,
      modifier = Modifier.width(56.dp).height(56.dp).shadow(6.dp, shape = CircleShape),
      shape = CircleShape,
      containerColor,
      contentColor) {
        content()
      }
}

@Preview
@Composable
fun NavigationUIButtonPreview() {
  Box(Modifier.background(Color.LightGray).padding(16.dp)) {
    NavigationUIButton({}) { Icon(Icons.Filled.Close, contentDescription = "Close") }
  }
}
