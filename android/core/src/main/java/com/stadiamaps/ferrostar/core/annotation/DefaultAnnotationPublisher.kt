package com.stadiamaps.ferrostar.core.annotation

import com.squareup.moshi.JsonAdapter
import com.stadiamaps.ferrostar.core.NavigationState
import uniffi.ferrostar.TripState

class DefaultAnnotationPublisher<T>(
    private val adapter: JsonAdapter<T>,
    private val speedLimitMapper: (T?) -> Speed?,
    private val onError: ((Throwable) -> Unit)? = null
) : AnnotationPublisher<T> {

  override fun map(state: NavigationState): AnnotationWrapper<T> {
    val annotations = decodeAnnotations(state)
    return AnnotationWrapper(annotations, speedLimitMapper(annotations))
  }

  private fun decodeAnnotations(state: NavigationState): T? {
    return if (state.tripState is TripState.Navigating) {
      try {
        state.tripState.annotationJson?.let { adapter.fromJson(it) }
      } catch (e: Exception) {
        onError?.invoke(e)
        null
      }
    } else {
      null
    }
  }
}
