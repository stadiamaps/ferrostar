package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable
import java.util.UUID

class SpokenInstruction : Record, Serializable {
    @Field
    var text: String = ""

    @Field
    var ssml: String? = null

    @Field
    var triggerDistanceBeforeManeuver: Double = 0.0

    @Field
    var utteranceId: String = UUID.randomUUID().toString()

    fun toSpokenInstruction(): uniffi.ferrostar.SpokenInstruction {
        return uniffi.ferrostar.SpokenInstruction(
            text = text,
            ssml = ssml,
            triggerDistanceBeforeManeuver = triggerDistanceBeforeManeuver,
            utteranceId = UUID.fromString(utteranceId)
        )
    }
}