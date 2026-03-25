package com.stadiamaps.ferrostar.maplibreui.runtime

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.runtime.paddingForGridView
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.OrnamentOptions

/**
 * Returns map options that keep the built-in ornaments clear of navigation overlays while leaving
 * gesture handling enabled.
 */
@Composable
internal fun rememberMapOptionsForProgressViewHeight(
    progressViewHeight: Dp = 0.dp,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 8.dp,
): MapOptions {
  val layoutDirection = LocalLayoutDirection.current
  val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
  val gridPadding = paddingForGridView()
  val windowInsetPadding = WindowInsets.systemBars.asPaddingValues()

  return remember(progressViewHeight, horizontalPadding, verticalPadding, isLandscape, gridPadding, windowInsetPadding) {
    val endPadding =
        windowInsetPadding.calculateEndPadding(layoutDirection) +
            gridPadding.calculateEndPadding(layoutDirection) +
            horizontalPadding
    val bottomPadding =
        windowInsetPadding.calculateBottomPadding() +
            gridPadding.calculateBottomPadding() +
            if (isLandscape) {
              verticalPadding
            } else {
              progressViewHeight + verticalPadding
            }

    MapOptions(
        ornamentOptions =
            OrnamentOptions(
                padding = PaddingValues(end = endPadding, bottom = bottomPadding),
                isCompassEnabled = false,
                isScaleBarEnabled = false,
                logoAlignment = Alignment.BottomStart,
                attributionAlignment = Alignment.BottomEnd,
            ),
    )
  }
}
