package com.stadiamaps.ferrostar.maplibreui.config

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
}
