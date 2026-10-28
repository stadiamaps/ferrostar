package com.stadiamaps.ferrostar.maplibreui

import org.junit.Assert.assertEquals
import org.junit.Test

class WrapLongitudeTest {
  @Test
  fun keepsLongitudesInsideTheWorld() {
    assertEquals(0.0, wrapLongitude(0.0), 0.0)
    assertEquals(16.37, wrapLongitude(16.37), 1e-9)
    assertEquals(-122.4, wrapLongitude(-122.4), 1e-9)
    assertEquals(-180.0, wrapLongitude(-180.0), 0.0)
  }

  @Test
  fun wrapsLongitudesFromOtherWorldCopies() {
    assertEquals(-180.0, wrapLongitude(180.0), 0.0)
    assertEquals(-170.0, wrapLongitude(190.0), 1e-9)
    assertEquals(170.0, wrapLongitude(-190.0), 1e-9)
    assertEquals(16.37, wrapLongitude(16.37 + 720.0), 1e-9)
    assertEquals(16.37, wrapLongitude(16.37 - 720.0), 1e-9)
  }
}
