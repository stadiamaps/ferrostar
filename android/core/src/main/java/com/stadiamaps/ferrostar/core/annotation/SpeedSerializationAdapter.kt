package com.stadiamaps.ferrostar.core.annotation

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

class SpeedSerializationAdapter : JsonAdapter<Speed>() {

  @ToJson
  override fun toJson(writer: JsonWriter, speed: Speed?) {
    if (speed == null) {
      writer.nullValue()
    } else {
      writer.beginObject()
      when (speed) {
        is Speed.NoLimit -> writer.name("none").value(true)
        is Speed.Unknown -> writer.name("unknown").value(true)
        is Speed.Value ->
            writer.name("value").value(speed.value).name("unit").value(speed.unit.text)
      }
      writer.endObject()
    }
  }

  @FromJson
  override fun fromJson(reader: JsonReader): Speed {
    reader.beginObject()
    var unknown: Boolean? = null
    var none: Boolean? = null
    var value: Double? = null
    var unit: String? = null

    while (reader.hasNext()) {
      when (reader.selectName(JsonReader.Options.of("none", "unknown", "value", "unit"))) {
        0 -> none = reader.nextBoolean()
        1 -> unknown = reader.nextBoolean()
        2 -> value = reader.nextDouble()
        3 -> unit = reader.nextString()
        else -> reader.skipName()
      }
    }
    reader.endObject()

    return if (none == true) {
      Speed.NoLimit
    } else if (unknown == true) {
      Speed.Unknown
    } else if (value != null && unit != null) {
      val speed = SpeedUnit.fromString(unit)
      if (speed != null) {
        Speed.Value(value, speed)
      } else {
        throw IllegalArgumentException("Invalid speed unit: $unit")
      }
    } else {
      throw IllegalArgumentException("Invalid max speed")
    }
  }
}
