package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Record
import expo.modules.kotlin.records.Field
import java.io.Serializable

class RouteStep : Record, Serializable {

    @Field
    var geometry: List<GeographicCoordinate> = emptyList()

    @Field
    var distance: Double = 0.0

    @Field
    var duration: Double = 0.0

    @Field
    var roadName: String? = null

    @Field
    var instruction: String = ""

    @Field
    var visualInstructions: List<VisualInstruction> = emptyList()

    @Field
    var spokenInstructions: List<SpokenInstruction> = emptyList()

    @Field
    var annotations: List<String>? = null

    fun toRouteStep(): uniffi.ferrostar.RouteStep {
        return uniffi.ferrostar.RouteStep(
            geometry = geometry.map { it.toGeographicCoordinate() },
            distance = distance,
            duration = duration,
            roadName = roadName,
            instruction = instruction,
            visualInstructions = visualInstructions.map { it.toVisualInstruction() },
            spokenInstructions = spokenInstructions.map { it.toSpokenInstruction() },
            annotations = annotations,
            incidents = emptyList()
        )
    }
}