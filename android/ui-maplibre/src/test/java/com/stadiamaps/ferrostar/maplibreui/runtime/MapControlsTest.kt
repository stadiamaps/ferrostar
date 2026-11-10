package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.maplibreui.NavigationMapOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.map.CameraConstraints
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.RenderOptions
import org.maplibre.compose.overlay.MapOverlay

class MapControlsTest {

  @Test
  fun mapOptionsForProgressViewHeightPreservesCallerOptions() {
    val baseOptions =
        NavigationMapOptions(
            cameraConstraints = CameraConstraints { maxZoom = 18.0 },
            renderOptions = RenderOptions.Debug,
            interactions = MapInteractions.None,
            uiOptions = MapUiOptions.None,
            overlay = MapOverlay.Full,
            overlayPadding = PaddingValues(99.dp),
        )

    val options =
        mapOptionsForProgressViewHeight(
            progressViewHeight = 40.dp,
            horizontalPadding = 16.dp,
            verticalPadding = 8.dp,
            contentPadding =
                PaddingValues(
                    start = 1.dp,
                    top = 2.dp,
                    end = 3.dp,
                    bottom = 4.dp,
                ),
            baseMapOptions = baseOptions,
            layoutDirection = LayoutDirection.Ltr,
            isLandscape = false,
            gridPadding =
                PaddingValues(
                    start = 5.dp,
                    top = 6.dp,
                    end = 7.dp,
                    bottom = 8.dp,
                ),
        )

    assertEquals(baseOptions.cameraConstraints, options.cameraConstraints)
    assertEquals(RenderOptions.Debug, options.renderOptions)
    assertEquals(MapInteractions.None, options.interactions)
    assertEquals(MapUiOptions.None, options.uiOptions)
    assertSame(MapOverlay.Full, options.overlay)
  }

  @Test
  fun mapOptionsForProgressViewHeightReplacesOverlayPadding() {
    val options =
        mapOptionsForProgressViewHeight(
            progressViewHeight = 40.dp,
            horizontalPadding = 16.dp,
            verticalPadding = 8.dp,
            contentPadding =
                PaddingValues(
                    start = 1.dp,
                    top = 2.dp,
                    end = 3.dp,
                    bottom = 4.dp,
                ),
            baseMapOptions = NavigationMapOptions(overlayPadding = PaddingValues(99.dp)),
            layoutDirection = LayoutDirection.Ltr,
            isLandscape = false,
            gridPadding =
                PaddingValues(
                    start = 5.dp,
                    top = 6.dp,
                    end = 7.dp,
                    bottom = 8.dp,
                ),
        )

    val padding = options.overlayPadding
    assertEquals(1.dp, padding.calculateStartPadding(LayoutDirection.Ltr))
    assertEquals(26.dp, padding.calculateEndPadding(LayoutDirection.Ltr))
    assertEquals(10.dp, padding.calculateTopPadding())
    assertEquals(60.dp, padding.calculateBottomPadding())
  }

  @Test
  fun mapOptionsForProgressViewHeightExcludesProgressHeightInLandscape() {
    val options =
        mapOptionsForProgressViewHeight(
            progressViewHeight = 40.dp,
            horizontalPadding = 16.dp,
            verticalPadding = 8.dp,
            contentPadding =
                PaddingValues(
                    start = 1.dp,
                    top = 2.dp,
                    end = 3.dp,
                    bottom = 4.dp,
                ),
            baseMapOptions = NavigationMapOptions(),
            layoutDirection = LayoutDirection.Ltr,
            isLandscape = true,
            gridPadding =
                PaddingValues(
                    start = 5.dp,
                    top = 6.dp,
                    end = 7.dp,
                    bottom = 8.dp,
                ),
        )

    assertEquals(20.dp, options.overlayPadding.calculateBottomPadding())
  }

  @Test
  fun mapOptionsForProgressViewHeightResolvesRtlPadding() {
    val options =
        mapOptionsForProgressViewHeight(
            progressViewHeight = 0.dp,
            horizontalPadding = 16.dp,
            verticalPadding = 8.dp,
            contentPadding =
                PaddingValues(
                    start = 1.dp,
                    end = 3.dp,
                ),
            baseMapOptions = NavigationMapOptions(),
            layoutDirection = LayoutDirection.Rtl,
            isLandscape = false,
            gridPadding =
                PaddingValues(
                    start = 5.dp,
                    end = 7.dp,
                ),
        )

    val padding = options.overlayPadding
    assertEquals(1.dp, padding.calculateStartPadding(LayoutDirection.Rtl))
    assertEquals(26.dp, padding.calculateEndPadding(LayoutDirection.Rtl))
  }

  @Test
  fun defaultNavigationMapOptionsShowOnlyTheAttribution() {
    assertSame(MapOverlay.AttributionOnly, NavigationMapOptions().overlay)
  }
}
