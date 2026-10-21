package com.stadiamaps.ferrostar.car.app.intent

import uniffi.ferrostar.GeographicCoordinate

/**
 * A map intent that [CarAppMapIntentParser] read from an [android.content.Intent].
 *
 * Each case matches one target in
 * [Google Maps intents](https://developer.android.com/guide/components/google-maps-intents) or
 * [Navigation app intents](https://developer.android.com/develop/devices/assistant/intents-assistant-nav-app).
 * Use a `when` to pick the behavior for each case.
 */
sealed interface CarAppMapIntent {

  /**
   * Display a map:
   * [`geo:lat,lng?z=zoom`](https://developer.android.com/guide/components/google-maps-intents#display-a-map).
   *
   * [zoom] is null when the URI has no valid `z`.
   */
  data class DisplayMap(val center: GeographicCoordinate, val zoom: Int?) : CarAppMapIntent

  /**
   * Search for a location:
   * [`geo:lat,lng?q=query`](https://developer.android.com/guide/components/google-maps-intents#search-for-a-location).
   *
   * [near] biases the search, and is null for `geo:0,0`. [zoom] is null when the URI has no valid
   * `z`.
   */
  data class Search(val query: String, val near: GeographicCoordinate?, val zoom: Int?) :
      CarAppMapIntent

  /**
   * Labeled pin:
   * [`geo:0,0?q=lat,lng(label)`](https://developer.android.com/guide/components/google-maps-intents#location-search).
   *
   * [label] is null when the label is empty or absent.
   */
  data class Pin(val location: GeographicCoordinate, val label: String?) : CarAppMapIntent

  /**
   * Navigation to a destination. The source is one of these:
   * - [`google.navigation:q=…`](https://developer.android.com/guide/components/google-maps-intents#launch-turn-by-turn-navigation)
   * - `geo:` with
   *   [`androidx.car.app.action.NAVIGATE`](https://developer.android.com/develop/devices/assistant/intents-assistant-nav-app#navigation-intent)
   *
   * [mode] is null when the URI has no known `mode`. [avoid] is empty when the URI has no known
   * `avoid` value.
   */
  data class Navigate(
      val destination: Destination,
      val action: Action,
      val mode: TravelMode?,
      val avoid: Set<Avoid>,
  ) : CarAppMapIntent {

    /** The `q` value or the `geo:` coordinates. */
    sealed interface Destination {
      /** A coordinate. [label] is the `q` value or the pin label, or null when there is none. */
      data class Coordinate(val location: GeographicCoordinate, val label: String?) : Destination

      /** The decoded `q` value. The app geocodes it. */
      data class Query(val query: String) : Destination
    }

    /**
     * The
     * [`intent`](https://developer.android.com/develop/devices/assistant/intents-assistant-nav-app#optional-parameters)
     * parameter.
     *
     * `google.navigation:` has no such parameter, so it is always [NAVIGATE].
     */
    enum class Action {
      /** `intent=navigation`: replace the destination and start navigation. */
      NAVIGATE,
      /** `intent=add_a_stop`: add the stop as the next destination. */
      ADD_STOP,
      /** `intent=directions`: show the route, but do not start navigation. */
      DIRECTIONS,
    }

    /** The `mode` parameter. */
    enum class TravelMode {
      DRIVING,
      BICYCLING,
      TWO_WHEELER,
      WALKING,
      /** Only in navigation app intents. */
      TAXI,
      /** Only in navigation app intents. */
      TRANSIT,
    }

    /** The `avoid` parameter. */
    enum class Avoid {
      TOLLS,
      HIGHWAYS,
      FERRIES,
    }
  }

  /**
   * Street View: `google.streetview:cbll=lat,lng` or
   * [`google.streetview:panoid=id`](https://developer.android.com/guide/components/google-maps-intents#display-a-street-view-panorama).
   */
  sealed interface StreetView : CarAppMapIntent {
    /** The panorama closest to [location]. */
    data class Location(val location: GeographicCoordinate) : StreetView

    /** The panorama with this [id]. `panoid` wins over `cbll`. */
    data class Panorama(val id: String) : StreetView
  }
}
