package com.stadiamaps.ferrostar.car.app

import android.content.Intent
import androidx.car.app.CarContext
import com.stadiamaps.ferrostar.car.app.intent.CarAppMapIntent
import com.stadiamaps.ferrostar.car.app.intent.CarAppMapIntent.Navigate
import com.stadiamaps.ferrostar.car.app.intent.CarAppMapIntent.Navigate.Action
import com.stadiamaps.ferrostar.car.app.intent.CarAppMapIntent.Navigate.Avoid
import com.stadiamaps.ferrostar.car.app.intent.CarAppMapIntent.Navigate.Destination
import com.stadiamaps.ferrostar.car.app.intent.CarAppMapIntent.Navigate.TravelMode
import com.stadiamaps.ferrostar.car.app.intent.CarAppMapIntentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uniffi.ferrostar.GeographicCoordinate

class CarAppMapIntentParserTest {

  private fun parse(action: String?, uri: String) =
      CarAppMapIntentParser.parse(action, uri.substringBefore(':'), uri.substringAfter(':'))

  private fun view(uri: String) = parse(Intent.ACTION_VIEW, uri)

  private fun navigate(uri: String) = parse(CarContext.ACTION_NAVIGATE, uri)

  private fun coordinate(lat: Double, lng: Double) = GeographicCoordinate(lat, lng)

  private fun navigation(
      destination: Destination,
      action: Action = Action.NAVIGATE,
      mode: TravelMode? = null,
      avoid: Set<Avoid> = emptySet(),
  ) = Navigate(destination, action, mode, avoid)

  // Google Maps intents: examples from
  // https://developer.android.com/guide/components/google-maps-intents

  @Test
  fun `display a map`() {
    assertEquals(
        CarAppMapIntent.DisplayMap(coordinate(37.7749, -122.4194), null),
        view("geo:37.7749,-122.4194"),
    )
  }

  @Test
  fun `display a map with zoom`() {
    assertEquals(
        CarAppMapIntent.DisplayMap(coordinate(37.7749, -122.4194), 11),
        view("geo:37.7749,-122.4194?z=11"),
    )
  }

  @Test
  fun `categorical search near the user`() {
    assertEquals(CarAppMapIntent.Search("restaurants", null, null), view("geo:0,0?q=restaurants"))
  }

  @Test
  fun `categorical search near a coordinate`() {
    assertEquals(
        CarAppMapIntent.Search("restaurants", coordinate(37.7749, -122.4194), null),
        view("geo:37.7749,-122.4194?q=restaurants"),
    )
  }

  @Test
  fun `categorical search with zoom`() {
    assertEquals(
        CarAppMapIntent.Search("restaurants", coordinate(37.7749, -122.4194), 10),
        view("geo:37.7749,-122.4194?z=10&q=restaurants"),
    )
  }

  @Test
  fun `address search`() {
    assertEquals(
        CarAppMapIntent.Search("1600 Amphitheatre Parkway, Mountain View, California", null, null),
        view("geo:0,0?q=1600 Amphitheatre Parkway, Mountain+View, California"),
    )
  }

  @Test
  fun `address search near a coordinate`() {
    assertEquals(
        CarAppMapIntent.Search("101 main street", coordinate(37.7749, -122.4194), null),
        view("geo:37.7749,-122.4194?q=101+main+street"),
    )
  }

  @Test
  fun `percent encoded search`() {
    assertEquals(
        CarAppMapIntent.Search("1st & Pike, Seattle", coordinate(37.7749, -122.4192), null),
        view("geo:37.7749,-122.4192?q=1st%20%26%20Pike%2C%20Seattle"),
    )
  }

  @Test
  fun `labeled pin`() {
    assertEquals(
        CarAppMapIntent.Pin(coordinate(-33.8666, 151.1957), "Google Sydney"),
        view("geo:0,0?q=-33.8666,151.1957(Google+Sydney)"),
    )
  }

  @Test
  fun `pin without label`() {
    assertEquals(
        CarAppMapIntent.Pin(coordinate(37.81, -122.42), null),
        view("geo:0,0?q=37.81,-122.42"),
    )
    assertEquals(
        CarAppMapIntent.Pin(coordinate(37.81, -122.42), null),
        view("geo:0,0?q=37.81,-122.42()"),
    )
  }

  @Test
  fun `pin label with parentheses`() {
    assertEquals(
        CarAppMapIntent.Pin(coordinate(37.81, -122.42), "Joe's (Downtown)"),
        view("geo:0,0?q=37.81,-122.42(Joe's+(Downtown))"),
    )
  }

  @Test
  fun `google navigation to an address`() {
    assertEquals(
        navigation(Destination.Query("Taronga Zoo, Sydney Australia")),
        view("google.navigation:q=Taronga+Zoo,+Sydney+Australia"),
    )
  }

  @Test
  fun `google navigation avoiding tolls and ferries`() {
    assertEquals(
        navigation(
            Destination.Query("Taronga Zoo, Sydney Australia"),
            avoid = setOf(Avoid.TOLLS, Avoid.FERRIES),
        ),
        view("google.navigation:q=Taronga+Zoo,+Sydney+Australia&avoid=tf"),
    )
  }

  @Test
  fun `google navigation by bicycle`() {
    assertEquals(
        navigation(Destination.Query("Taronga Zoo, Sydney Australia"), mode = TravelMode.BICYCLING),
        view("google.navigation:q=Taronga+Zoo,+Sydney+Australia&mode=b"),
    )
  }

  @Test
  fun `google navigation by two wheeler`() {
    assertEquals(
        navigation(
            Destination.Query("Connaught Place, New Delhi,Delhi"),
            mode = TravelMode.TWO_WHEELER,
        ),
        view("google.navigation:q=Connaught+Place,+New+Delhi,Delhi&mode=l"),
    )
  }

  @Test
  fun `google navigation to a coordinate`() {
    assertEquals(
        navigation(Destination.Coordinate(coordinate(37.81, -122.42), null)),
        view("google.navigation:q=37.81,-122.42"),
    )
  }

  @Test
  fun `google navigation does not depend on the intent action`() {
    val expected = navigation(Destination.Query("Pier 39"))
    assertEquals(expected, parse(null, "google.navigation:q=Pier+39"))
    assertEquals(expected, navigate("google.navigation:q=Pier+39"))
  }

  @Test
  fun `google navigation ignores the intent parameter`() {
    assertEquals(
        navigation(Destination.Query("Pier 39")),
        view("google.navigation:q=Pier+39&intent=add_a_stop"),
    )
  }

  @Test
  fun `street view at a coordinate`() {
    assertEquals(
        CarAppMapIntent.StreetView.Location(coordinate(46.414382, 10.013988)),
        view("google.streetview:cbll=46.414382,10.013988"),
    )
  }

  @Test
  fun `street view panorama`() {
    assertEquals(
        CarAppMapIntent.StreetView.Panorama("Iaa2JyfIggYAAAQfCZU9KQ"),
        view("google.streetview:panoid=Iaa2JyfIggYAAAQfCZU9KQ"),
    )
  }

  @Test
  fun `street view ignores the camera`() {
    assertEquals(
        CarAppMapIntent.StreetView.Location(coordinate(29.9774614, 31.1329645)),
        view("google.streetview:cbll=29.9774614,31.1329645&cbp=0,30,0,0,-15"),
    )
  }

  @Test
  fun `street view panorama wins over coordinate`() {
    assertEquals(
        CarAppMapIntent.StreetView.Panorama("abc"),
        view("google.streetview:cbll=46.414382,10.013988&panoid=abc"),
    )
  }

  // Navigation app intents: examples from
  // https://developer.android.com/develop/devices/assistant/intents-assistant-nav-app

  @Test
  fun `navigate to a query`() {
    assertEquals(navigation(Destination.Query("Googleplex")), navigate("geo:0,0?q=Googleplex"))
  }

  @Test
  fun `navigate add a stop by bicycle`() {
    assertEquals(
        navigation(
            Destination.Query("1600 Amphitheatre parkway"),
            action = Action.ADD_STOP,
            mode = TravelMode.BICYCLING,
        ),
        navigate("geo:0,0?q=1600+Amphitheatre+parkway&mode=b&intent=add_a_stop"),
    )
  }

  @Test
  fun `navigate on foot`() {
    assertEquals(
        navigation(Destination.Query("coffee shop"), mode = TravelMode.WALKING),
        navigate("geo:0,0?q=coffee+shop&mode=w&intent=navigation"),
    )
  }

  @Test
  fun `navigate to a coordinate with a query label`() {
    assertEquals(
        navigation(
            Destination.Coordinate(coordinate(1.1, 2.2), "Starbucks on Main Street"),
            mode = TravelMode.WALKING,
        ),
        navigate("geo:1.1,2.2?q=Starbucks+on+Main+Street&mode=w&intent=navigation"),
    )
  }

  @Test
  fun `navigate to a coordinate`() {
    assertEquals(
        navigation(Destination.Coordinate(coordinate(1.1, 2.2), null), mode = TravelMode.WALKING),
        navigate("geo:1.1,2.2?mode=w&intent=navigation"),
    )
  }

  @Test
  fun `navigate add a stop at a coordinate`() {
    assertEquals(
        navigation(
            Destination.Coordinate(
                coordinate(47.61594547836694, -122.20373173098756),
                "575 Bellevue Square, Bellevue, WA 98004",
            ),
            action = Action.ADD_STOP,
        ),
        navigate(
            "geo:47.61594547836694,-122.20373173098756" +
                "?q=575+Bellevue+Square,+Bellevue,+WA+98004&intent=add_a_stop"
        ),
    )
  }

  @Test
  fun `navigate for directions only`() {
    assertEquals(
        navigation(Destination.Query("Googleplex"), action = Action.DIRECTIONS),
        navigate("geo:0,0?q=Googleplex&intent=directions"),
    )
  }

  @Test
  fun `navigate avoiding tolls and ferries`() {
    assertEquals(
        navigation(Destination.Query("googleplex"), avoid = setOf(Avoid.TOLLS, Avoid.FERRIES)),
        navigate("geo:0,0?q=googleplex&avoid=tf"),
    )
  }

  @Test
  fun `navigate by transit and taxi`() {
    assertEquals(
        navigation(Destination.Query("Googleplex"), mode = TravelMode.TRANSIT),
        navigate("geo:0,0?q=Googleplex&mode=r"),
    )
    assertEquals(
        navigation(Destination.Query("Googleplex"), mode = TravelMode.TAXI),
        navigate("geo:0,0?q=Googleplex&mode=x"),
    )
  }

  @Test
  fun `navigate ignores entry`() {
    assertEquals(
        navigation(
            Destination.Coordinate(coordinate(47.61594547836694, -122.20373173098756), null)
        ),
        navigate("geo:47.61594547836694,-122.20373173098756?entry=assistant"),
    )
  }

  @Test
  fun `navigate to a labeled coordinate query`() {
    assertEquals(
        navigation(Destination.Coordinate(coordinate(37.81, -122.42), "Pier 39")),
        navigate("geo:0,0?q=37.81,-122.42(Pier+39)"),
    )
  }

  @Test
  fun `non car navigate action`() {
    assertEquals(
        navigation(Destination.Query("Googleplex")),
        parse("android.intent.action.NAVIGATE", "geo:0,0?q=Googleplex"),
    )
  }

  @Test
  fun `search intent`() {
    assertEquals(
        CarAppMapIntent.Search("restaurants nearby", null, null),
        view("geo:0,0?q=restaurants+nearby"),
    )
  }

  // Edge cases

  @Test
  fun `geo without an action is a view`() {
    assertEquals(CarAppMapIntent.Search("Pier 39", null, null), parse(null, "geo:0,0?q=Pier+39"))
  }

  @Test
  fun `geo without a coordinate or query is null`() {
    assertNull(view("geo:0,0"))
    assertNull(view("geo:0,0?z=11"))
    assertNull(view("geo:0,0?q="))
    assertNull(view("geo:0,0?q=+"))
    assertNull(navigate("geo:0,0"))
    assertNull(navigate("geo:0,0?mode=d"))
  }

  @Test
  fun `geo ignores altitude and uri parameters`() {
    val expected = CarAppMapIntent.DisplayMap(coordinate(37.786971, -122.399677), null)
    assertEquals(expected, view("geo:37.786971,-122.399677,100"))
    assertEquals(expected, view("geo:37.786971,-122.399677;u=35"))
  }

  @Test
  fun `invalid coordinates are not coordinates`() {
    assertNull(view("geo:91,0"))
    assertNull(view("geo:0,181"))
    assertNull(view("geo:NaN,NaN"))
    assertNull(view("geo:abc"))
    assertEquals(CarAppMapIntent.Search("Pier 39", null, null), view("geo:91,0?q=Pier+39"))
    assertEquals(
        CarAppMapIntent.Search("91,0(Nowhere)", null, null),
        view("geo:0,0?q=91,0(Nowhere)"),
    )
  }

  @Test
  fun `invalid zoom is null`() {
    assertEquals(
        CarAppMapIntent.DisplayMap(coordinate(37.81, -122.42), null),
        view("geo:37.81,-122.42?z=abc"),
    )
  }

  @Test
  fun `bad percent escape stays as it is`() {
    assertEquals(CarAppMapIntent.Search("100%25%zz", null, null), view("geo:0,0?q=100%25%zz"))
  }

  @Test
  fun `first query parameter wins`() {
    assertEquals(CarAppMapIntent.Search("first", null, null), view("geo:0,0?q=first&q=second"))
  }

  @Test
  fun `query value may contain an equals sign`() {
    assertEquals(CarAppMapIntent.Search("a=b", null, null), view("geo:0,0?q=a=b"))
  }

  @Test
  fun `unknown mode avoid and intent values`() {
    assertEquals(
        navigation(Destination.Query("Googleplex"), avoid = setOf(Avoid.TOLLS)),
        navigate("geo:0,0?q=Googleplex&mode=z&avoid=tz&intent=teleport"),
    )
  }

  @Test
  fun `scheme is case insensitive`() {
    assertEquals(
        CarAppMapIntent.DisplayMap(coordinate(37.81, -122.42), null),
        view("GEO:37.81,-122.42"),
    )
  }

  @Test
  fun `missing or unknown parts are null`() {
    assertNull(view("google.navigation:mode=d"))
    assertNull(view("google.navigation:q="))
    assertNull(view("google.streetview:cbp=0,30,0,0,-15"))
    assertNull(view("google.streetview:cbll=91,0"))
    assertNull(view("https://example.com"))
    assertNull(CarAppMapIntentParser.parse(Intent.ACTION_VIEW, null, null))
    assertNull(CarAppMapIntentParser.parse(Intent.ACTION_VIEW, "geo", null))
  }
}
