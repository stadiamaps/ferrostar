package com.stadiamaps.ferrostar.core.annotation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object SpeedSerializer : KSerializer<Speed> {
  override val descriptor: SerialDescriptor =
      buildClassSerialDescriptor("Speed") {
        element<Boolean>("none", isOptional = true)
        element<Boolean>("unknown", isOptional = true)
        element<Double>("speed", isOptional = true)
        element<String>("unit", isOptional = true)
      }

  override fun serialize(encoder: Encoder, value: Speed) {
    val composite = encoder.beginStructure(descriptor)
    when (value) {
      is Speed.NoLimit -> composite.encodeBooleanElement(descriptor, 0, true)
      is Speed.Unknown -> composite.encodeBooleanElement(descriptor, 1, true)
      is Speed.Value -> {
        composite.encodeDoubleElement(descriptor, 2, value.value)
        composite.encodeStringElement(descriptor, 3, value.unit.text)
      }
    }
    composite.endStructure(descriptor)
  }

  override fun deserialize(decoder: Decoder): Speed {
    val dec = decoder.beginStructure(descriptor)
    var none: Boolean? = null
    var unknown: Boolean? = null
    var value: Double? = null
    var unit: String? = null

    loop@ while (true) {
      when (dec.decodeElementIndex(descriptor)) {
        0 -> none = dec.decodeBooleanElement(descriptor, 0)
        1 -> unknown = dec.decodeBooleanElement(descriptor, 1)
        2 -> value = dec.decodeDoubleElement(descriptor, 2)
        3 -> unit = dec.decodeStringElement(descriptor, 3)
        CompositeDecoder.DECODE_DONE -> break@loop
        else -> {
          /* Skip unknown elements */
        }
      }
    }
    dec.endStructure(descriptor)

    return when {
      none == true -> Speed.NoLimit
      unknown == true -> Speed.Unknown
      value != null && unit != null -> {
        val speedUnit = SpeedUnit.fromString(unit)
        requireNotNull(speedUnit) { "Invalid speed unit: $unit" }
        Speed.Value(value, speedUnit)
      }

      else -> throw IllegalArgumentException("Invalid max speed")
    }
  }
}
