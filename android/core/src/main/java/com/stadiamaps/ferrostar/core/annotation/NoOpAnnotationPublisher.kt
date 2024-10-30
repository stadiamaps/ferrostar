package com.stadiamaps.ferrostar.core.annotation

import com.stadiamaps.ferrostar.core.NavigationState

class NoOpAnnotationPublisher : AnnotationPublisher<Any> {
  override fun map(state: NavigationState): AnnotationWrapper<Any> {
    return AnnotationWrapper(state = state)
  }
}
