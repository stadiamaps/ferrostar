package com.stadiamaps.ferrostar.core

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteAdapterInterface
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WellKnownRouteProvider

/** An abstraction around the various ways of getting routes. */
sealed class RouteProvider {
  /**
   * A route provider optimized for a request/response model such as HTTP or socket communications.
   */
  class RouteAdapter(val adapter: RouteAdapterInterface) : RouteProvider()

  /**
   * A provider commonly used for local route generation. Extensible for any sort of custom route
   * generation that doesn't fit the [RouteAdapter] use case.
   */
  class CustomProvider(val provider: CustomRouteProvider) : RouteProvider()
}

/**
 * A custom route provider is a generic asynchronous route generator.
 *
 * The typical use case for a custom route provider is local route generation, but it is generally
 * useful for any route generation that doesn't involve a standardized request generation (ex: HTTP
 * POST) -> request execution (eg: okhttp3) -> response parsing (stream of bytes) flow.
 *
 * This applies well to offline route generation, since you are not getting back a stream of bytes
 * (ex: from a socket) that need decoding, but rather a data structure from a function call which
 * just needs mapping into the Ferrostar route model.
 */
fun interface CustomRouteProvider {
  suspend fun getRoutes(userLocation: UserLocation, waypoints: List<Waypoint>): List<Route>
}

fun WellKnownRouteProvider.withJsonOptions(jsonOptions: Map<String, Any>): WellKnownRouteProvider {
  return when (this) {
    is WellKnownRouteProvider.Valhalla ->
        WellKnownRouteProvider.Valhalla(endpointUrl, profile, jsonOptions.toJson())
    is WellKnownRouteProvider.GraphHopper ->
        WellKnownRouteProvider.GraphHopper(
            endpointUrl, profile, locale, voiceUnits, jsonOptions.toJson())
  }
}

private val json = Json { ignoreUnknownKeys = true }

private fun Map<String, Any?>.toJsonElement(): JsonElement = Json.parseToJsonElement(this.toJson())

private fun Map<String, Any?>.toJson(): String =
    json.encodeToString(
        MapSerializer(String.serializer(), JsonElement.serializer()),
        mapValues { (_, v) ->
          when (v) {
            is String -> Json.encodeToJsonElement(String.serializer(), v)
            is Int -> Json.encodeToJsonElement(Int.serializer(), v)
            is Boolean -> Json.encodeToJsonElement(Boolean.serializer(), v)
            is Double -> Json.encodeToJsonElement(Double.serializer(), v)
            is Float -> Json.encodeToJsonElement(Float.serializer(), v)
            is Long -> Json.encodeToJsonElement(Long.serializer(), v)
            is Map<*, *> -> {
              @Suppress("UNCHECKED_CAST")
              (v as? Map<String, Any>)?.toJsonElement()
                  ?: throw IllegalArgumentException("Unsupported map value type: ${v::class}")
            }
            null -> Json.encodeToJsonElement(String.serializer(), "null")
            else -> throw IllegalArgumentException("Unsupported value type: ${v::class}")
          }
        })
