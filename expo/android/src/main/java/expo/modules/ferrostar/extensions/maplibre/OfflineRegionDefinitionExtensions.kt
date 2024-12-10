package expo.modules.ferrostar.extensions.maplibre

import com.mapbox.mapboxsdk.offline.OfflineRegionDefinition
import expo.modules.ferrostar.records.BoundingBox
import expo.modules.ferrostar.records.GeographicCoordinate

fun OfflineRegionDefinition.toExpoOfflineRegionDefinition(): expo.modules.ferrostar.records.maplibre.OfflineRegionDefinition {
    // Create a new expo record version of the offline region definition
    val definition = expo.modules.ferrostar.records.maplibre.OfflineRegionDefinition()
    definition.styleURL = this.styleURL
    definition.includeIdeographs = this.includeIdeographs
    definition.type = this.type
    definition.maxZoom = this.maxZoom
    definition.minZoom = this.minZoom
    definition.pixelRatio = this.pixelRatio
    definition.bounds = BoundingBox()
    definition.bounds.sw = GeographicCoordinate()
    definition.bounds.sw.lat =
        this.bounds?.southWest?.latitude ?: 0.0
    definition.bounds.sw.lng =
        this.bounds?.southWest?.longitude ?: 0.0
    definition.bounds.ne = GeographicCoordinate()
    definition.bounds.ne.lat =
        this.bounds?.northEast?.latitude ?: 0.0
    definition.bounds.ne.lng =
        this.bounds?.northEast?.longitude ?: 0.0
    return definition
}