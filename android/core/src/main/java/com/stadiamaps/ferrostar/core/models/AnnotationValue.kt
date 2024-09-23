package com.stadiamaps.ferrostar.core.models

import com.squareup.moshi.Json

data class AnnotationValue(
  val distance: Double,
  val duration: Double,
  @Json(name = "max_speed_mps")
  val maxSpeedMps: Double?,
  @Json(name = "speed_mps")
  val speedMps: Double
)