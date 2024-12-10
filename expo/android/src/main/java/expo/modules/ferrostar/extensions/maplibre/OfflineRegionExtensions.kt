package expo.modules.ferrostar.extensions.maplibre

import com.mapbox.mapboxsdk.offline.OfflineRegion

fun OfflineRegion.toExpoOfflineRegion(): expo.modules.ferrostar.records.maplibre.OfflineRegion {
    val expoOfflineRegion = expo.modules.ferrostar.records.maplibre.OfflineRegion()
    expoOfflineRegion.id = this.id
    expoOfflineRegion.metadata = this.metadata.toString(Charsets.UTF_8)
    expoOfflineRegion.isDeliveringInactiveMessages = this.isDeliveringInactiveMessages
    expoOfflineRegion.definition = this.definition.toExpoOfflineRegionDefinition()

    return expoOfflineRegion
}