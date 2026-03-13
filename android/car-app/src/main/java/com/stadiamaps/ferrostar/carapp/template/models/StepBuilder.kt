package com.stadiamaps.ferrostar.carapp.template.models

import android.content.Context
import androidx.car.app.navigation.model.Lane
import androidx.car.app.navigation.model.LaneDirection
import androidx.car.app.navigation.model.Step
import uniffi.ferrostar.DrivingSide
import uniffi.ferrostar.LaneInfo
import uniffi.ferrostar.VisualInstruction

/**
 * Builds a Car App Library [Step] from a Ferrostar [VisualInstruction].
 *
 * @param context The context used to resolve maneuver icon drawables.
 * @param drivingSide The driving side, used for roundabout direction.
 * @param roundaboutExitNumber The roundabout exit number, if applicable.
 */
fun VisualInstruction.toCarStep(
    context: Context,
    drivingSide: DrivingSide = DrivingSide.RIGHT,
    roundaboutExitNumber: Int? = null
): Step {
  val icon = primaryContent.toCarIcon(context)
  val maneuver = primaryContent.toCarManeuver(icon, drivingSide, roundaboutExitNumber)
  val builder = Step.Builder(primaryContent.text).setManeuver(maneuver)

  secondaryContent?.text?.let { builder.setRoad(it) }

  primaryContent.laneInfo?.forEach { lane -> builder.addLane(lane.toCarLane()) }

  return builder.build()
}

/** Converts a Ferrostar [LaneInfo] to a Car App Library [Lane]. */
fun LaneInfo.toCarLane(): Lane {
  val builder = Lane.Builder()
  for (direction in directions) {
    val shape = direction.toLaneShape()
    val isRecommended = active && direction == activeDirection
    builder.addDirection(LaneDirection.create(shape, isRecommended))
  }
  return builder.build()
}
