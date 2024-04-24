package com.stadiamaps.ferrostar

import com.stadiamaps.ferrostar.composeui.roundToNearest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundToNearestTest {
  @Test
  fun `Round to nearest integer`() {
    assertEquals(1.0, 1.0.roundToNearest(1), Double.MIN_VALUE)
    assertEquals(1.0, 0.5.roundToNearest(1), Double.MIN_VALUE)
    assertEquals(12.0, 11.6.roundToNearest(1), Double.MIN_VALUE)
  }

  @Test
  fun `Round to nearest 5`() {
    assertEquals(0.0, 1.0.roundToNearest(5), Double.MIN_VALUE)
    assertEquals(5.0, 3.0.roundToNearest(5), Double.MIN_VALUE)
    assertEquals(5.0, 7.0.roundToNearest(5), Double.MIN_VALUE)
    assertEquals(10.0, 8.0.roundToNearest(5), Double.MIN_VALUE)
  }

  @Test
  fun `Round to nearest 10`() {
    assertEquals(0.0, 1.0.roundToNearest(10), Double.MIN_VALUE)
    assertEquals(0.0, 3.0.roundToNearest(10), Double.MIN_VALUE)
    assertEquals(10.0, 7.0.roundToNearest(10), Double.MIN_VALUE)
    assertEquals(30.0, 28.0.roundToNearest(10), Double.MIN_VALUE)
  }

  @Test
  fun `Round to nearest 100`() {
    assertEquals(0.0, 1.0.roundToNearest(100), Double.MIN_VALUE)
    assertEquals(0.0, 40.0.roundToNearest(100), Double.MIN_VALUE)
    assertEquals(100.0, 50.0.roundToNearest(100), Double.MIN_VALUE)
    assertEquals(300.0, 280.0.roundToNearest(100), Double.MIN_VALUE)
  }
}
