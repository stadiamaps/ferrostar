package expo.modules.ferrostar.records.maplibre

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

class OfflineRegion : Record {
    @Field
    var definition: OfflineRegionDefinition = OfflineRegionDefinition()

    @Field
    var id: Long = 0L

    @Field
    var metadata: String = ""

    @Field
    var isDeliveringInactiveMessages: Boolean = false
}