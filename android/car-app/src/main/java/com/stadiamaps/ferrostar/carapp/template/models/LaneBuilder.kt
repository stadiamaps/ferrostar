package com.stadiamaps.ferrostar.carapp.template.models

import androidx.car.app.navigation.model.Lane
import androidx.car.app.navigation.model.LaneDirection
import uniffi.ferrostar.LaneInfo
fun LaneInfo.toCarLane(): Lane =
  Lane.Builder()
      .apply {
        for (direction in directions) {
          val shape = LaneInfo.asLaneShape(direction)
          val isRecommended = active && direction == activeDirection
          addDirection(LaneDirection.create(shape, isRecommended))
        }
      }
      .build()
fun LaneInfo.Companion.asLaneShape(indications: String): Int =
    when (indications) {
      "uturn" -> LaneDirection.SHAPE_U_TURN_LEFT
      "sharp right" -> LaneDirection.SHAPE_SHARP_RIGHT
      "right" -> LaneDirection.SHAPE_NORMAL_RIGHT
      "slight right" -> LaneDirection.SHAPE_SLIGHT_RIGHT
      "straight" -> LaneDirection.SHAPE_STRAIGHT
      "slight left" -> LaneDirection.SHAPE_SLIGHT_LEFT
      "left" -> LaneDirection.SHAPE_NORMAL_LEFT
      "sharp left" -> LaneDirection.SHAPE_SHARP_LEFT
      else -> LaneDirection.SHAPE_UNKNOWN
    }
