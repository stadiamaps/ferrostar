package expo.modules.ferrostar.records.maplibre

import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition
import expo.modules.ferrostar.records.BoundingBox
import expo.modules.ferrostar.records.GeographicCoordinate
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable

class OfflineRegionDefinition : Record, Serializable {
    @Field
    var bounds: BoundingBox = BoundingBox()

    @Field
    var includeIdeographs: Boolean = false

    @Field
    var maxZoom: Double = 0.0

    @Field
    var minZoom: Double = 0.0

    @Field
    var pixelRatio: Float = 0.0f

    @Field
    var styleURL: String? = null

    @Field
    var type: String = "tileregion"

    fun toOfflineRegionDefinition(): com.mapbox.mapboxsdk.offline.OfflineRegionDefinition {
        val latLngBounds = LatLngBounds.from(
            bounds.ne.lat,
            bounds.ne.lng,
            bounds.sw.lat,
            bounds.sw.lng
        )
        return OfflineTilePyramidRegionDefinition(styleURL, latLngBounds, minZoom, maxZoom, pixelRatio, includeIdeographs)
    }
}