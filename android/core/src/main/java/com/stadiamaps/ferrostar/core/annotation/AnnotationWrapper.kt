package com.stadiamaps.ferrostar.core.annotation

import com.stadiamaps.ferrostar.core.NavigationState

data class AnnotationWrapper<T>(
    val annotation: T? = null,
    val speed: Speed? = null,
    val state: NavigationState
)
