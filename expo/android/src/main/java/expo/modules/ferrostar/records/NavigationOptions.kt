package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable

class NavigationOptions: Record, Serializable {
    @Field
    var id: String? = null

    @Field
    var styleUrl: String = "https://demotiles.maplibre.org/style.json"

    @Field
    var snapUserLocationToRoute: Boolean = false
}