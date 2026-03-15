package com.stadiamaps.ferrostar.composeui.runtime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun paddingForGridView(horizontal: Dp = 16.dp, vertical: Dp = 16.dp): PaddingValues {
  val rawLayoutDirection = LocalConfiguration.current.layoutDirection
  val layoutDirection: LayoutDirection = LayoutDirection.entries[rawLayoutDirection]
  val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()

  val topPadding =
      if (safeDrawingPadding.calculateTopPadding() > vertical) {
        0.dp
      } else {
        vertical
      }

  val bottomPadding =
      if (safeDrawingPadding.calculateBottomPadding() > vertical) {
        0.dp
      } else {
        vertical
      }

  val systemStartPadding = safeDrawingPadding.calculateStartPadding(layoutDirection)
  val startPadding =
      if (systemStartPadding > horizontal) {
        systemStartPadding
      } else {
        horizontal
      }

  val systemEndPadding = safeDrawingPadding.calculateEndPadding(layoutDirection)
  val endPadding =
      if (systemEndPadding > horizontal) {
        systemEndPadding
      } else {
        horizontal
      }

  return PaddingValues(
      top = topPadding, start = startPadding, end = endPadding, bottom = bottomPadding)
}
