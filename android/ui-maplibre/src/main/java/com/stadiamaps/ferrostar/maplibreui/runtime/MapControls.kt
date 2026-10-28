package com.stadiamaps.ferrostar.maplibreui.runtime

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.runtime.paddingForGridView
import com.stadiamaps.ferrostar.maplibreui.NavigationMapOptions
import org.maplibre.compose.overlay.MapOverlay

/**
 * Returns map options whose overlay padding keeps the map controls clear of navigation overlays.
 */
@Composable
internal fun rememberMapOptionsForProgressViewHeight(
    progressViewHeight: Dp = 0.dp,
    // The map overlay adds MapOverlay.Spacing inside this padding.
    horizontalPadding: Dp = 16.dp - MapOverlay.Spacing,
    verticalPadding: Dp = 8.dp - MapOverlay.Spacing,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    baseMapOptions: NavigationMapOptions = NavigationMapOptions(),
): NavigationMapOptions {
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

internal fun mapOptionsForProgressViewHeight(
    progressViewHeight: Dp,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    contentPadding: PaddingValues,
    baseMapOptions: NavigationMapOptions,
    layoutDirection: LayoutDirection,
    isLandscape: Boolean,
    gridPadding: PaddingValues,
): NavigationMapOptions {
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
      overlayPadding =
          PaddingValues(
              start = startPadding,
              end = endPadding,
              top = topPadding,
              bottom = bottomPadding,
          ),
  )
}
