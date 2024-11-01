package com.stadiamaps.ferrostar.composeui.views.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PillDragHandle(
    isExpanded: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth(),
    iconTintColor: Color = MaterialTheme.colorScheme.onSurface,
    toggle: () -> Unit = {}
) {
  val handleHeight = if (isExpanded) 36.dp else 4.dp
  Box(modifier = modifier.height(handleHeight).clickable(onClick = toggle)) {
    if (isExpanded) {
      Icon(
          Icons.Rounded.KeyboardArrowUp,
          modifier = Modifier.align(Alignment.Center),
          contentDescription = "Show upcoming maneuvers",
          tint = iconTintColor)
    } else {
      Box(
          modifier =
              Modifier.align(Alignment.Center)
                  .height(handleHeight)
                  .width(24.dp)
                  .background(iconTintColor, RoundedCornerShape(6.dp))
                  .semantics {
                    role = Role.Button
                    onClick(label = "Hide upcoming maneuvers") {
                      toggle()
                      true
                    }
                  })
    }
  }
}

@Preview
@Composable
fun PreviewPillDragHandleCollapsed() {
  PillDragHandle(isExpanded = false, iconTintColor = Color.White)
}

@Preview
@Composable
fun PreviewPillDragHandleExpanded() {
  PillDragHandle(isExpanded = true, iconTintColor = Color.White)
}
