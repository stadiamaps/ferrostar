package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable

class VisualInstruction : Record, Serializable {
    @Field
    var primaryContent: VisualInstructionContent = VisualInstructionContent()

    @Field
    var secondaryContent: VisualInstructionContent? = null

    @Field
    var subContent: VisualInstructionContent? = null

    @Field
    var triggerDistanceBeforeManeuver: Double = 0.0

    fun toVisualInstruction(): uniffi.ferrostar.VisualInstruction {
        return uniffi.ferrostar.VisualInstruction(
            primaryContent = primaryContent.toVisualInstructionContent(),
            secondaryContent = secondaryContent?.toVisualInstructionContent(),
            subContent = subContent?.toVisualInstructionContent(),
            triggerDistanceBeforeManeuver = triggerDistanceBeforeManeuver
        )
    }
}