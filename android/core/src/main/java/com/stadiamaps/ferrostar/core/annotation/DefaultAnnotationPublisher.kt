package com.stadiamaps.ferrostar.core.annotation

import com.stadiamaps.ferrostar.core.NavigationState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import uniffi.ferrostar.TripState

class DefaultAnnotationPublisher<T>(
    private val json: Json,
    private val serializer: KSerializer<T>,
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
        state.tripState.annotationJson?.let { json.decodeFromString(serializer, it) }
      } catch (e: Exception) {
        onError?.invoke(e)
        null
      }
    } else {
      null
    }
  }
}
