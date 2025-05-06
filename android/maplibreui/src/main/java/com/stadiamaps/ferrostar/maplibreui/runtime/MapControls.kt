package com.stadiamaps.ferrostar.maplibreui.runtime

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maplibre.compose.settings.AttributionSettings
import com.maplibre.compose.settings.CompassSettings
import com.maplibre.compose.settings.LogoSettings
import com.maplibre.compose.settings.MapControlPosition
import com.maplibre.compose.settings.MapControls
import com.stadiamaps.ferrostar.composeui.runtime.paddingForGridView

/**
 * Returns the map controls for the given configuration.
 *
 * @param progressViewHeight The height of the progress view.
 * @param horizontalPadding The horizontal padding to apply to the map controls. Defaults to 16.dp
 *   to match the default padding of the progress view.
 * @param verticalPadding The vertical padding to apply to the map controls from the top of the
 *   progress view. Defaults to 8.dp.
 *
 * TODO: This function is attempting to optimize the map controls for many screen sizes, system
 *   bars, and orientations. We should remain open to feedback for specific cases.
 * TODO: Remove this suppress lint w/ https://issuetracker.google.com/issues/349411310
 */
@SuppressLint("ProduceStateDoesNotAssignValue")
@Composable
internal fun rememberMapControlsForProgressViewHeight(
    progressViewHeight: Dp = 0.dp,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 8.dp
): State<MapControls> {
  val layoutDirection = LocalLayoutDirection.current
  val density = LocalDensity.current
  val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

  val gridPadding = paddingForGridView()
  val windowInsetPadding = WindowInsets.systemBars.asPaddingValues()

  return produceState(
      initialValue = MapControls(),
      key1 = progressViewHeight,
      key2 = gridPadding,
      key3 = windowInsetPadding) {
        // This calculation clamps the controls to the trailing edge of the screen in landscape mode
        // with less padding in that case. The reason for this is that with edge-to-edge, there's
        // a larger map canvas available.
        val endPaddingDp =
            windowInsetPadding.calculateEndPadding(layoutDirection) +
                gridPadding.calculateEndPadding(layoutDirection)
        val endOffsetDp = if (isLandscape) endPaddingDp else horizontalPadding

        val bottomPaddingDp =
            windowInsetPadding.calculateBottomPadding() + gridPadding.calculateBottomPadding()
        val bottomOffsetDp =
            if (isLandscape) bottomPaddingDp else bottomPaddingDp + progressViewHeight

        // TODO: This could be improved if we want to add pixel width to dp conversion in
        //  maplibre-compose.
        val attributionOffset = 24.dp

        value =
            MapControls(
                attribution =
                    AttributionSettings.initWithLayoutAndPosition(
                        layoutDirection,
                        density,
                        position =
                            MapControlPosition.BottomEnd(
                                horizontal = endOffsetDp,
                                vertical = bottomOffsetDp + verticalPadding)),
                compass = CompassSettings(enabled = false),
                logo =
                    LogoSettings.initWithLayoutAndPosition(
                        layoutDirection,
                        density,
                        position =
                            MapControlPosition.BottomEnd(
                                horizontal = endOffsetDp + attributionOffset,
                                vertical = bottomOffsetDp + verticalPadding)))
      }
}
