package com.stadiamaps.ferrostar.core.annotation

sealed class Speed {
  data object NoLimit : Speed()

  data object Unknown : Speed()

  data class Value(val value: Double, val unit: SpeedUnit) : Speed()
}
