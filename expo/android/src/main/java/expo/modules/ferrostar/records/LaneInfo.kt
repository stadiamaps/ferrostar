package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable

class LaneInfo : Record, Serializable {
    @Field
    var active: Boolean = false

    @Field
    var directions: List<String> = emptyList()

    @Field
    var activeDirection: String? = null

    fun toLaneInfo(): uniffi.ferrostar.LaneInfo {
        return uniffi.ferrostar.LaneInfo(
            active = active,
            directions = directions,
            activeDirection = activeDirection
        )
    }
}