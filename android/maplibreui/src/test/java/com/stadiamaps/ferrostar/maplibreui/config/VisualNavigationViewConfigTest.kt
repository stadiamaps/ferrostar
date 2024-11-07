package com.stadiamaps.ferrostar.maplibreui.config

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.config.buttonSize
import com.stadiamaps.ferrostar.composeui.config.useMuteButton
import com.stadiamaps.ferrostar.composeui.config.useZoomButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VisualNavigationViewConfigTest {

  @Test
  fun testInit() {
    val config = VisualNavigationViewConfig()
    assertFalse(config.showMute)
    assertFalse(config.showZoom)
  }

  @Test
  fun testDefault() {
    val config = VisualNavigationViewConfig.Default()
    assert(config.showMute)
    assert(config.showZoom)
  }

  @Test
  fun testUseMuteButton() {
    val config = VisualNavigationViewConfig().useMuteButton()
    assert(config.showMute)
  }

  @Test
  fun testUseZoomButton() {
    val config = VisualNavigationViewConfig().useZoomButton()
    assert(config.showZoom)
  }

  @Test
  fun testUseMuteButtonAndZoomButton() {
    val config = VisualNavigationViewConfig().useMuteButton().useZoomButton()
    assert(config.showMute)
    assert(config.showZoom)
  }

  @Test
  fun testButtonSize() {
    val newSize = DpSize(42.dp, 42.dp)
    val config = VisualNavigationViewConfig().buttonSize(newSize)
    assertEquals(newSize, config.buttonSize)
  }
}
