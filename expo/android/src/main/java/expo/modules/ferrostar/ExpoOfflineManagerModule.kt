package expo.modules.ferrostar

import android.content.Context
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineRegion
import expo.modules.ferrostar.extensions.maplibre.toExpoOfflineRegion
import expo.modules.ferrostar.records.maplibre.OfflineRegionDefinition
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class CreateOfflineRegionCallback(val onCreateCallback: (OfflineRegion) -> Unit, val onErrorCallback: (String) -> Unit) : OfflineManager.CreateOfflineRegionCallback {
    override fun onCreate(offlineRegion: OfflineRegion) {
        onCreateCallback(offlineRegion)
    }

    override fun onError(error: String) {
        onErrorCallback(error)
    }
}

class FileSourceCallback(val onSuccessCallback: () -> Unit, val onErrorCallback: (String) -> Unit) : OfflineManager.FileSourceCallback {
    override fun onSuccess() {
        onSuccessCallback()
    }

    override fun onError(message: String) {
        onErrorCallback(message)
    }
}

class ListOfflineRegionsCallback(val onErrorCallback: (String) -> Unit, val onListCallback: (Array<OfflineRegion>?) -> Unit) : OfflineManager.ListOfflineRegionsCallback {
     override fun onError(error: String) {
        onErrorCallback(error)
    }

    override fun onList(offlineRegions: Array<OfflineRegion>?) {
        onListCallback(offlineRegions)
    }
}

class ExpoOfflineManagerModule : Module() {
    override fun definition() = ModuleDefinition {
        Name("ExpoOfflineManager")

        AsyncFunction("createOfflineRegion") { definition: OfflineRegionDefinition, metadata: String, promise: Promise ->
            val context: Context = appContext.reactContext ?: throw Exceptions.ReactContextLost()
            OfflineManager.Companion.getInstance(context).createOfflineRegion(definition.toOfflineRegionDefinition(),
                metadata.toByteArray(),
                CreateOfflineRegionCallback(
                    { offlineRegion ->
                        promise.resolve(offlineRegion.toExpoOfflineRegion())
                    }, { error ->
                        promise.reject("createOfflineRegion", error, null)
                    })
            )
        }

        AsyncFunction("resetDatabase") { promise: Promise ->
            val context: Context = appContext.reactContext ?: throw Exceptions.ReactContextLost()
            OfflineManager.Companion.getInstance(context).resetDatabase(FileSourceCallback({
                promise.resolve(null)
            }, { error ->
                promise.reject("resetDatabase", error, null)
            }))
        }

        AsyncFunction("packDatabase") { promise: Promise ->
            val context: Context = appContext.reactContext ?: throw Exceptions.ReactContextLost()
            OfflineManager.Companion.getInstance(context).packDatabase(FileSourceCallback({
                promise.resolve(null)
            }, { error ->
                promise.reject("packDatabase", error, null)
            }))
        }

        Function("runPackDatabaseAutomatically") { autopack: Boolean ->
            val context: Context = appContext.reactContext ?: throw Exceptions.ReactContextLost()
            OfflineManager.Companion.getInstance(context).runPackDatabaseAutomatically(autopack)
        }

        AsyncFunction("setMaximumAmbientCacheSize") { size: Long, promise: Promise ->
            val context: Context = appContext.reactContext ?: throw Exceptions.ReactContextLost()
            OfflineManager.Companion.getInstance(context).setMaximumAmbientCacheSize(size, FileSourceCallback({
                promise.resolve(null)
            }, { error ->
                promise.reject("setMaximumAmbientCacheSize", error, null)
            }))
        }

        AsyncFunction("listOfflineRegions") { promise: Promise ->
            val context: Context = appContext.reactContext ?: throw Exceptions.ReactContextLost()
            OfflineManager.Companion.getInstance(context).listOfflineRegions(ListOfflineRegionsCallback({ error ->
                promise.reject("listOfflineRegions", error, null)
            }, { offlineRegions ->
                promise.resolve(offlineRegions?.map { it.toExpoOfflineRegion() })
            }))
        }
    }
}