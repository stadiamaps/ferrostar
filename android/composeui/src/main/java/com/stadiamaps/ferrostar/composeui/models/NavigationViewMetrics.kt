package com.stadiamaps.ferrostar.composeui.models

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

data class NavigationViewMetrics(
  val progressViewSize: DpSize,
  val instructionsViewSize: DpSize,
  val buttonSize: DpSize,
) {

  /**
   * Returns the MapView's safe insets.
   *
   * @param start Optional additional start padding. Default is 0.dp.
   * @param top Optional additional top padding. Default is instructionsViewSize.height.
   * @param end Optional additional end padding. Default is 0.dp.
   * @param bottom Optional additional bottom padding. Default is progressViewSize.height.
   * @return The calculated padding insets.
   */
  fun mapViewInsets(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp,
  ): PaddingValues {
    return PaddingValues(
      start = start,
      top = instructionsViewSize.height + top,
      end = end,
      bottom = progressViewSize.height + buttonSize.height + bottom,
    )
  }
}