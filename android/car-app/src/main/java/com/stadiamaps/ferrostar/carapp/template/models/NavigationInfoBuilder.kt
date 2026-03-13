package com.stadiamaps.ferrostar.carapp.template.models

import android.content.Context
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Trip
import uniffi.ferrostar.DrivingSide
import uniffi.ferrostar.TripProgress
import uniffi.ferrostar.VisualInstruction

/**
 * Builds a Car App Library [RoutingInfo] for use with navigation templates.
 *
 * @param instruction The current visual instruction.
 * @param progress The current trip progress, or null if unavailable.
 * @param context The context used to resolve maneuver icon drawables.
 * @param drivingSide The driving side for roundabout direction.
 * @param roundaboutExitNumber The roundabout exit number, if applicable.
 */
fun buildNavigationInfo(
    instruction: VisualInstruction,
    progress: TripProgress?,
    context: Context,
    drivingSide: DrivingSide = DrivingSide.RIGHT,
    roundaboutExitNumber: Int? = null
): RoutingInfo {
  val step = instruction.toCarStep(context, drivingSide, roundaboutExitNumber)
  val distance = progress.toCarDistanceToNextManeuver()
  return RoutingInfo.Builder().setCurrentStep(step, distance).build()
}

/**
 * Builds a Car App Library [Trip] for use with [NavigationManager.updateTrip].
 *
 * Returns null when [instruction] is null (nothing to display).
 *
 * @param instruction The current visual instruction, or null if not yet available.
 * @param progress The current trip progress, or null if unavailable.
 * @param context The context used to resolve maneuver icon drawables.
 * @param drivingSide The driving side for roundabout direction.
 * @param roundaboutExitNumber The roundabout exit number, if applicable.
 */
fun buildNavigationTrip(
    instruction: VisualInstruction?,
    progress: TripProgress?,
    context: Context,
    drivingSide: DrivingSide = DrivingSide.RIGHT,
    roundaboutExitNumber: Int? = null,
    destinationName: String? = null
): Trip? {
  instruction ?: return null
  progress ?: return null

  val step = instruction.toCarStep(context, drivingSide, roundaboutExitNumber)
  val stepTravelEstimate = progress.toCarTravelEstimate()
  val tripBuilder = Trip.Builder()
  tripBuilder.addStep(step, stepTravelEstimate)

  val destination = Destination.Builder().setName(destinationName ?: "Destination").build()
  tripBuilder.addDestination(destination, stepTravelEstimate)

  return tripBuilder.build()
}
