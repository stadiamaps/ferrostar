package com.stadiamaps.ferrostar.maplibreui.runtime

import android.util.Log
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.compose.camera.CameraState

private const val MAP_READY_TAG = "NavigationMapView"

/**
 * Extracts the native [Style] from the compose [CameraState] via reflection. This is a workaround
 * because maplibre-compose (tested against 0.12.1) does not expose a public API for accessing the
 * underlying style after the map is ready. The function degrades gracefully — if the internal API
 * changes, it returns null and logs a warning.
 */
internal fun CameraState.nativeStyleOrNull(): Style? {
  val mapAdapter =
      runCatching {
            javaClass.getMethod("getMap\$maplibre_compose").invoke(this)
          }
          .onFailure {
            Log.w(MAP_READY_TAG, "Unable to read compose map adapter for onMapReadyCallback", it)
          }
          .getOrNull() ?: return null

  val rawMap =
      runCatching {
            val field = mapAdapter.javaClass.getDeclaredField("map")
            field.isAccessible = true
            field.get(mapAdapter) as? MapLibreMap
          }
          .onFailure {
            Log.w(MAP_READY_TAG, "Unable to read MapLibreMap from compose adapter", it)
          }
          .getOrNull()

  return rawMap?.style.also { style ->
    if (style == null) {
      Log.w(MAP_READY_TAG, "onMapReadyCallback could not access native Style from compose map")
    }
  }
}
