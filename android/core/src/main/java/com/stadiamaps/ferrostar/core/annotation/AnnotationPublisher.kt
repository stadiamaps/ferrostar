package com.stadiamaps.ferrostar.core.annotation

import com.stadiamaps.ferrostar.core.NavigationState

interface AnnotationPublisher<T> {
  fun map(state: NavigationState): AnnotationWrapper<T>
}
