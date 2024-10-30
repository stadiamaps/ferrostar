package com.stadiamaps.ferrostar.core.extensions

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
      TripState.Idle -> null
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
        TripState.Idle -> null
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
      TripState.Idle -> null
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
      TripState.Idle -> null
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
      TripState.Idle -> null
    }
