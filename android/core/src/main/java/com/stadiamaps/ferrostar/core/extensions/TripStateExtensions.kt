package com.stadiamaps.ferrostar.core.extensions

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import uniffi.ferrostar.TripState
import java.nio.charset.StandardCharsets

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
 * Get the route annotations at the user's current location.
 *
 * This function will deserialize the annotation bytes provided by the trip
 * state.
 *
 * @param type The json data class that annotations should be deserialized to.
 * @return The annotation data or null if the trip is not navigating.
 */
inline fun <reified T> TripState.annotation(type: Class<T>): T? =
    when (this) {
      is TripState.Navigating -> {
        this.annotationBytes?.let {
          val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
          val jsonAdapter: JsonAdapter<T> = moshi.adapter(type)
          val jsonString = it.toString(Charsets.UTF_8)
          jsonAdapter.fromJson(jsonString)
        }
      }
      is TripState.Complete,
      TripState.Idle -> null
    }