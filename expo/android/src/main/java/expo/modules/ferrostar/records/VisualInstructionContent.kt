package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import expo.modules.kotlin.types.Enumerable
import java.io.Serializable

class VisualInstructionContent : Record, Serializable {
    @Field
    var text: String = ""

    @Field
    var maneuverType: ManeuverType? = null

    @Field
    var maneuverModifier: ManeuverModifier? = null

    @Field
    var roundaboutExitDegrees: Int? = null

    @Field
    var laneInfo: List<LaneInfo>? = null

    fun toVisualInstructionContent(): uniffi.ferrostar.VisualInstructionContent {
        return uniffi.ferrostar.VisualInstructionContent(
            text = text,
            if (maneuverType != null) uniffi.ferrostar.ManeuverType.valueOf(maneuverType?.name ?: "") else null,
            if (maneuverModifier != null) uniffi.ferrostar.ManeuverModifier.valueOf(maneuverModifier?.name ?: "") else null,
            roundaboutExitDegrees = roundaboutExitDegrees?.toUShort(),
            laneInfo = laneInfo?.map { it.toLaneInfo() }
        )
    }
}