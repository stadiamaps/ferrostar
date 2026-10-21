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
   */
  data class DisplayMap(val center: GeographicCoordinate, val zoom: Int?) : CarAppMapIntent

  /**
   * Search for a location:
   * [`geo:lat,lng?q=query`](https://developer.android.com/guide/components/google-maps-intents#search-for-a-location).
   *
   * @param near The coordinate that biases the search, or null for `geo:0,0`.
   */
  data class Search(val query: String, val near: GeographicCoordinate?, val zoom: Int?) :
      CarAppMapIntent

  /**
   * Labeled pin:
   * [`geo:0,0?q=lat,lng(label)`](https://developer.android.com/guide/components/google-maps-intents#location-search).
   */
  data class Pin(val location: GeographicCoordinate, val label: String?) : CarAppMapIntent

  /**
   * Navigation to a destination. The source is one of these:
   * - [`google.navigation:q=…`](https://developer.android.com/guide/components/google-maps-intents#launch-turn-by-turn-navigation)
   * - `geo:` with
   *   [`androidx.car.app.action.NAVIGATE`](https://developer.android.com/develop/devices/assistant/intents-assistant-nav-app#navigation-intent)
   */
  data class Navigate(
      val destination: Destination,
      val action: Action,
      val mode: TravelMode?,
      val avoid: Set<Avoid>,
  ) : CarAppMapIntent {

    /** The `q` value or the `geo:` coordinates. */
    sealed interface Destination {
      data class Coordinate(val location: GeographicCoordinate, val label: String?) : Destination

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
    data class Location(val location: GeographicCoordinate) : StreetView

    /** The doc says `panoid` wins over `cbll`. */
    data class Panorama(val id: String) : StreetView
  }
}
