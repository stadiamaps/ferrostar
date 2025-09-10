package com.stadiamaps.ferrostar.core.extensions

import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.TripState

/**
 * Get the progress of the trip while navigating.
 *
 * @return The progress of the trip, or null if the trip is not navigating.
 */
fun TripState.progress() =
    when (this) {
      is TripState.Navigating -> this.progress
      is TripState.Complete,
      is TripState.Idle -> null
    }

/**
 * Get the visual instruction for the current step.
 *
 * @return The visual instruction for the current step or null.
 */
fun TripState.visualInstruction() =
    try {
      when (this) {
        is TripState.Navigating -> this.visualInstruction
        is TripState.Complete,
        is TripState.Idle -> null
      }
    } catch (_: NoSuchElementException) {
      null
    }

/**
 * Get the deviation handler from the trip.
 *
 * @return The deviation handler if navigating or null.
 */
fun TripState.deviation() =
    when (this) {
      is TripState.Navigating -> this.deviation
      is TripState.Complete,
      is TripState.Idle -> null
    }

/**
 * Get the current road name.
 *
 * @return The current road name (if available and navigating).
 */
fun TripState.currentRoadName() =
    when (this) {
      is TripState.Navigating ->
          this.remainingSteps.firstOrNull()?.roadName.let {
            if (it.isNullOrBlank()) {
              null
            } else {
              it
            }
          }
      is TripState.Complete,
      is TripState.Idle -> null
    }

/**
 * Get the current step geometry index - closest coordinate to the user's snapped location This
 * index is relative to the *current* [`RouteStep`]'s geometry.
 *
 * @return The current step geometry index (if available and navigating).
 */
fun TripState.currentStepGeometryIndex() =
    when (this) {
      is TripState.Navigating -> this.currentStepGeometryIndex?.toInt()
      is TripState.Complete,
      is TripState.Idle -> null
    }

/**
 * Get the remaining steps (including the current) in the current trip.
 *
 * @return The list of remaining steps (if any).
 */
fun TripState.remainingSteps() =
    when (this) {
      is TripState.Navigating -> this.remainingSteps
      is TripState.Complete,
      is TripState.Idle -> null
    }

/**
 * Get the remaining waypoints (starting at the *next* waypoint "goal") in the current trip.
 *
 * @return The list of remaining waypoints (if any).
 */
fun TripState.remainingWaypoints() =
    when (this) {
      is TripState.Navigating -> this.remainingWaypoints
      is TripState.Complete,
      is TripState.Idle -> null
    }

/**
 * Get the UI's preferred representation of User's location from the trip state.
 *
 * This will return the snapped user location if there is no deviation from the route. If the user
 * has deviated, it will return the user's actual raw location, allowing the puck to deviate from
 * the route line.
 *
 * @return The user location (if available and navigating).
 */
fun TripState.preferredUserLocation() =
    when (this) {
      is TripState.Navigating -> {
        when (this.deviation) {
          is RouteDeviation.NoDeviation -> this.snappedUserLocation
          is RouteDeviation.OffRoute -> this.userLocation
        }
      }
      is TripState.Idle -> this.userLocation
      is TripState.Complete -> this.userLocation
    }
