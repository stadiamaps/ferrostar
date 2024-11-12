package com.stadiamaps.ferrostar.core.annotation.valhalla

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.stadiamaps.ferrostar.core.annotation.AnnotationPublisher
import com.stadiamaps.ferrostar.core.annotation.DefaultAnnotationPublisher
import com.stadiamaps.ferrostar.core.annotation.SpeedSerializationAdapter

fun valhallaExtendedOSRMAnnotationPublisher(): AnnotationPublisher<ValhallaOSRMExtendedAnnotation> {
  val moshi =
      Moshi.Builder().add(SpeedSerializationAdapter()).add(KotlinJsonAdapterFactory()).build()
  val adapter = moshi.adapter(ValhallaOSRMExtendedAnnotation::class.java)
  return DefaultAnnotationPublisher<ValhallaOSRMExtendedAnnotation>(adapter) { it?.speedLimit }
}
