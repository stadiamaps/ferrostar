package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.map.RenderOptions

class MapControlsTest {

  @Test
  fun mapOptionsForProgressViewHeightPreservesCallerOptions() {
    val baseOptions =
        MapOptions(
            renderOptions = RenderOptions.Debug,
            gestureOptions = GestureOptions.AllDisabled,
            ornamentOptions =
                OrnamentOptions(
                    padding = PaddingValues(99.dp),
                    isLogoEnabled = false,
                    logoAlignment = Alignment.TopEnd,
                    isAttributionEnabled = false,
                    attributionAlignment = Alignment.TopStart,
                    isCompassEnabled = true,
                    compassAlignment = Alignment.BottomStart,
                    isScaleBarEnabled = true,
                    scaleBarAlignment = Alignment.BottomEnd,
                ),
        )

    val options =
        mapOptionsForProgressViewHeight(
            progressViewHeight = 40.dp,
            horizontalPadding = 16.dp,
            verticalPadding = 8.dp,
            contentPadding = PaddingValues(
                start = 1.dp,
                top = 2.dp,
                end = 3.dp,
                bottom = 4.dp,
            ),
            baseMapOptions = baseOptions,
            layoutDirection = LayoutDirection.Ltr,
            isLandscape = false,
            gridPadding = PaddingValues(
                start = 5.dp,
                top = 6.dp,
                end = 7.dp,
                bottom = 8.dp,
            ),
        )

    assertEquals(RenderOptions.Debug, options.renderOptions)
    assertEquals(GestureOptions.AllDisabled, options.gestureOptions)

    val ornamentOptions = options.ornamentOptions
    assertFalse(ornamentOptions.isLogoEnabled)
    assertEquals(Alignment.TopEnd, ornamentOptions.logoAlignment)
    assertFalse(ornamentOptions.isAttributionEnabled)
    assertEquals(Alignment.TopStart, ornamentOptions.attributionAlignment)
    assertTrue(ornamentOptions.isCompassEnabled)
    assertEquals(Alignment.BottomStart, ornamentOptions.compassAlignment)
    assertTrue(ornamentOptions.isScaleBarEnabled)
    assertEquals(Alignment.BottomEnd, ornamentOptions.scaleBarAlignment)
  }

  @Test
  fun mapOptionsForProgressViewHeightReplacesOrnamentPadding() {
    val options =
        mapOptionsForProgressViewHeight(
            progressViewHeight = 40.dp,
            horizontalPadding = 16.dp,
            verticalPadding = 8.dp,
            contentPadding = PaddingValues(
                start = 1.dp,
                top = 2.dp,
                end = 3.dp,
                bottom = 4.dp,
            ),
            baseMapOptions =
                MapOptions(ornamentOptions = OrnamentOptions(padding = PaddingValues(99.dp))),
            layoutDirection = LayoutDirection.Ltr,
            isLandscape = false,
            gridPadding = PaddingValues(
                start = 5.dp,
                top = 6.dp,
                end = 7.dp,
                bottom = 8.dp,
            ),
        )

    val padding = options.ornamentOptions.padding
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
            contentPadding = PaddingValues(
                start = 1.dp,
                top = 2.dp,
                end = 3.dp,
                bottom = 4.dp,
            ),
            baseMapOptions = MapOptions(),
            layoutDirection = LayoutDirection.Ltr,
            isLandscape = true,
            gridPadding = PaddingValues(
                start = 5.dp,
                top = 6.dp,
                end = 7.dp,
                bottom = 8.dp,
            ),
        )

    assertEquals(20.dp, options.ornamentOptions.padding.calculateBottomPadding())
  }

  @Test
  fun mapOptionsForProgressViewHeightResolvesRtlPadding() {
    val options =
        mapOptionsForProgressViewHeight(
            progressViewHeight = 0.dp,
            horizontalPadding = 16.dp,
            verticalPadding = 8.dp,
            contentPadding = PaddingValues(
                start = 1.dp,
                end = 3.dp,
            ),
            baseMapOptions = MapOptions(),
            layoutDirection = LayoutDirection.Rtl,
            isLandscape = false,
            gridPadding = PaddingValues(
                start = 5.dp,
                end = 7.dp,
            ),
        )

    val padding = options.ornamentOptions.padding
    assertEquals(1.dp, padding.calculateStartPadding(LayoutDirection.Rtl))
    assertEquals(26.dp, padding.calculateEndPadding(LayoutDirection.Rtl))
  }
}
