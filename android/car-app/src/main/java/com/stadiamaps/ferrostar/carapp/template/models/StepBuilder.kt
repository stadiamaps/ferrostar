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
    drivingSide: DrivingSide,
    roundaboutExitNumber: Int?
): Step {
  val maneuver = primaryContent.toCarManeuver(context, drivingSide, roundaboutExitNumber)
  return Step.Builder(primaryContent.text)
      .setManeuver(maneuver)
      .apply {
        secondaryContent?.text?.let {
          setRoad(it)
        }
        primaryContent.laneInfo?.forEach { lane ->
          addLane(lane.toCarLane())
        }
      }
      .build()
}
