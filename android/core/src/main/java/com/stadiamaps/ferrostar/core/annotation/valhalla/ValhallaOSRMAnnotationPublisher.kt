package com.stadiamaps.ferrostar.core.annotation.valhalla

import com.stadiamaps.ferrostar.core.annotation.AnnotationPublisher
import com.stadiamaps.ferrostar.core.annotation.DefaultAnnotationPublisher
import kotlinx.serialization.json.Json

fun valhallaExtendedOSRMAnnotationPublisher(): AnnotationPublisher<ValhallaOSRMExtendedAnnotation> =
    DefaultAnnotationPublisher(
        json = Json { ignoreUnknownKeys = true },
        serializer = ValhallaOSRMExtendedAnnotation.serializer(),
        speedLimitMapper = { it?.speedLimit })
