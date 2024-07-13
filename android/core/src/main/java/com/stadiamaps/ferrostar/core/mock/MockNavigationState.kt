package com.stadiamaps.ferrostar.core.mock

import com.stadiamaps.ferrostar.core.NavigationState
import java.time.Instant
import uniffi.ferrostar.CourseOverGround
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.Speed
import uniffi.ferrostar.TripProgress
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.VisualInstruction
import uniffi.ferrostar.VisualInstructionContent

fun UserLocation.Companion.pedestrianExample(): UserLocation {
  return UserLocation(
      GeographicCoordinate(37.81, -122.42),
      horizontalAccuracy = 1.0,
      courseOverGround = CourseOverGround(90u, 1u),
      timestamp = Instant.now(),
      speed = Speed(1.0, 1.0))
}

fun NavigationState.Companion.pedestrianExample(): NavigationState {
  return NavigationState(
      tripState =
          TripState.Navigating(
              snappedUserLocation = UserLocation.pedestrianExample(),
              remainingSteps = listOf(),
              remainingWaypoints = listOf(),
              progress =
                  TripProgress(
                      distanceToNextManeuver = 0.0,
                      distanceRemaining = 0.0,
                      durationRemaining = 0.0),
              deviation = RouteDeviation.NoDeviation,
              visualInstruction =
                  VisualInstruction(
                      primaryContent =
                          VisualInstructionContent(
                              text = "Head east",
                              maneuverType = ManeuverType.TURN,
                              maneuverModifier = ManeuverModifier.RIGHT,
                              roundaboutExitDegrees = null),
                      secondaryContent = null,
                      triggerDistanceBeforeManeuver = 0.0,
                  ),
              spokenInstruction = null),
      routeGeometry = listOf(),
      isCalculatingNewRoute = false)
}
