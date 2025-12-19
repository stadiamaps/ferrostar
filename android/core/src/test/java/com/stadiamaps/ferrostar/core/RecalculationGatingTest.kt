package com.stadiamaps.ferrostar.core

import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecalculationGatingTest {
  @Test
  fun shouldReturnTrueWhenTheDiffIsMoreThan5Seconds() {
    val minimumCooldown = 5.seconds

    val tenSecondsAgo = System.nanoTime() - 10.seconds.inWholeNanoseconds
    val moreThanFiveSecondsAgo = System.nanoTime() - 5.1.seconds.inWholeNanoseconds

    assertTrue(hasWaitedMinimumRecalculationDelay(tenSecondsAgo, minimumCooldown))
    assertTrue(hasWaitedMinimumRecalculationDelay(moreThanFiveSecondsAgo, minimumCooldown))
  }

  @Test
  fun shouldReturnFalseWhenTheDiffIsMoreLess5Seconds() {
    val minimumCooldown = 5.seconds

    val fourSecondsAgo = System.nanoTime() - 4.seconds.inWholeNanoseconds
    val threeSecondsAgo = System.nanoTime() - 3.seconds.inWholeNanoseconds
    val twoSecondsAgo = System.nanoTime() - 2.seconds.inWholeNanoseconds
    val oneSecondAgo = System.nanoTime() - 1.seconds.inWholeNanoseconds

    assertFalse(hasWaitedMinimumRecalculationDelay(fourSecondsAgo, minimumCooldown))
    assertFalse(hasWaitedMinimumRecalculationDelay(threeSecondsAgo, minimumCooldown))
    assertFalse(hasWaitedMinimumRecalculationDelay(twoSecondsAgo, minimumCooldown))
    assertFalse(hasWaitedMinimumRecalculationDelay(oneSecondAgo, minimumCooldown))
  }
}
