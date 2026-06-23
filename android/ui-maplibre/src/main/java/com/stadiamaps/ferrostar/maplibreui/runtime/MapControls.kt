package com.stadiamaps.ferrostar.maplibreui.runtime

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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
    contentPadding: PaddingValues = PaddingValues(0.dp),
    baseMapOptions: MapOptions = defaultNavigationMapOptions(),
): MapOptions {
  val layoutDirection = LocalLayoutDirection.current
  val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
  val gridPadding = paddingForGridView()

  return remember(
      progressViewHeight,
      horizontalPadding,
      verticalPadding,
      contentPadding,
      baseMapOptions,
      layoutDirection,
      isLandscape,
      gridPadding,
  ) {
    mapOptionsForProgressViewHeight(
        progressViewHeight = progressViewHeight,
        horizontalPadding = horizontalPadding,
        verticalPadding = verticalPadding,
        contentPadding = contentPadding,
        baseMapOptions = baseMapOptions,
        layoutDirection = layoutDirection,
        isLandscape = isLandscape,
        gridPadding = gridPadding,
    )
  }
}

internal fun defaultNavigationMapOptions(): MapOptions =
    MapOptions(
        ornamentOptions =
            OrnamentOptions(
                isCompassEnabled = false,
                isScaleBarEnabled = false,
                logoAlignment = Alignment.BottomStart,
                attributionAlignment = Alignment.BottomEnd,
            ),
    )

internal fun mapOptionsForProgressViewHeight(
    progressViewHeight: Dp,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    contentPadding: PaddingValues,
    baseMapOptions: MapOptions,
    layoutDirection: LayoutDirection,
    isLandscape: Boolean,
    gridPadding: PaddingValues,
): MapOptions {
  val startPadding = contentPadding.calculateStartPadding(layoutDirection)
  val endPadding =
      contentPadding.calculateEndPadding(layoutDirection) +
          gridPadding.calculateEndPadding(layoutDirection) +
          horizontalPadding
  val topPadding = contentPadding.calculateTopPadding() + verticalPadding
  val bottomPadding =
      contentPadding.calculateBottomPadding() +
          gridPadding.calculateBottomPadding() +
          if (isLandscape) {
            verticalPadding
          } else {
            progressViewHeight + verticalPadding
          }

  return baseMapOptions.copy(
      ornamentOptions =
          baseMapOptions.ornamentOptions.copy(
              padding =
                  PaddingValues(
                      start = startPadding,
                      end = endPadding,
                      top = topPadding,
                      bottom = bottomPadding,
                  ),
          ),
  )
}
