package com.stadiamaps.ferrostar.core.annotation

import com.stadiamaps.ferrostar.core.NavigationState

class NoOpAnnotationPublisher : AnnotationPublisher<Unit> {
  override fun map(state: NavigationState): AnnotationWrapper<Unit> {
    return AnnotationWrapper()
  }
}
