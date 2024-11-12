package com.stadiamaps.ferrostar.core.annotation

import com.squareup.moshi.JsonAdapter
import com.stadiamaps.ferrostar.core.NavigationState
import uniffi.ferrostar.TripState

class DefaultAnnotationPublisher<T>(
    private val adapter: JsonAdapter<T>,
    private val speedLimitMapper: (T?) -> Speed?,
) : AnnotationPublisher<T> {

  override fun map(state: NavigationState): AnnotationWrapper<T> {
    val annotations = decodeAnnotations(state)
    return AnnotationWrapper(annotations, speedLimitMapper(annotations), state)
  }

  private fun decodeAnnotations(state: NavigationState): T? {
    return if (state.tripState is TripState.Navigating) {
      val json = state.tripState.annotationJson
      if (json != null) {
        adapter.fromJson(json)
      } else {
        null
      }
    } else {
      null
    }
  }
}
