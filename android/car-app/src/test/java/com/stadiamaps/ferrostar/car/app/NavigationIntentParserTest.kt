package com.stadiamaps.ferrostar.car.app

import android.net.Uri
import com.stadiamaps.ferrostar.car.app.intent.NavigationDestination
import com.stadiamaps.ferrostar.car.app.intent.NavigationIntentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationIntentParserTest {

  // parseUri — exercises opaque URI parsing without Uri.getQueryParameter()

  @Test
  fun `parseUri geo coordinates`() {
    val result = NavigationIntentParser().parseUri(Uri.parse("geo:37.81,-122.42"))
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
    assertNull(result.query)
  }

  @Test
  fun `parseUri geo coordinates with altitude`() {
    val result = NavigationIntentParser().parseUri(Uri.parse("geo:37.81,-122.42,100"))
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
  }

  @Test
  fun `parseUri geo query only`() {
    val result = NavigationIntentParser().parseUri(Uri.parse("geo:0,0?q=coffee+shops"))
    assertNotNull(result)
    assertNull(result!!.latitude)
    assertNull(result.longitude)
    assertEquals("coffee shops", result.query)
  }

  @Test
  fun `parseUri geo percent-encoded query`() {
    val result = NavigationIntentParser().parseUri(Uri.parse("geo:0,0?q=Caf%C3%A9%20Roma"))
    assertNotNull(result)
    assertEquals("Café Roma", result!!.query)
  }

  @Test
  fun `parseUri google navigation coordinates`() {
    val result = NavigationIntentParser().parseUri(Uri.parse("google.navigation:q=37.81,-122.42"))
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
    assertNull(result.query)
  }

  @Test
  fun `parseUri google navigation place name with plus encoding`() {
    val result =
        NavigationIntentParser().parseUri(Uri.parse("google.navigation:q=Golden+Gate+Bridge"))
    assertNotNull(result)
    assertNull(result!!.latitude)
    assertEquals("Golden Gate Bridge", result.query)
  }

  @Test
  fun `parseUri unrecognized scheme returns null`() {
    assertNull(NavigationIntentParser().parseUri(Uri.parse("https://example.com")))
  }

  // decodeQueryValue

  @Test
  fun `decodeQueryValue converts plus to space`() {
    assertEquals("Golden Gate Bridge", NavigationIntentParser.decodeQueryValue("Golden+Gate+Bridge"))
  }

  @Test
  fun `decodeQueryValue handles percent encoding`() {
    assertEquals("Café", NavigationIntentParser.decodeQueryValue("Caf%C3%A9"))
  }

  // parseGeoSsp

  @Test
  fun `geo coordinates only`() {
    val result = NavigationIntentParser.parseGeoSsp("37.8100,-122.4200", null)
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
    assertNull(result.query)
  }

  @Test
  fun `geo coordinates with altitude ignored`() {
    val result = NavigationIntentParser.parseGeoSsp("37.8100,-122.4200,100", null)
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
  }

  @Test
  fun `geo query only`() {
    val result = NavigationIntentParser.parseGeoSsp("0,0", "coffee shops")
    assertNotNull(result)
    assertNull(result!!.latitude)
    assertNull(result.longitude)
    assertEquals("coffee shops", result.query)
  }

  @Test
  fun `geo coordinates and query`() {
    val result = NavigationIntentParser.parseGeoSsp("37.81,-122.42", "Pier 39")
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
    assertEquals("Pier 39", result.query)
  }

  @Test
  fun `geo zero coordinates with query uses query`() {
    val result = NavigationIntentParser.parseGeoSsp("0,0", "Starbucks")
    assertNotNull(result)
    assertNull(result!!.latitude)
    assertNull(result.longitude)
    assertEquals("Starbucks", result.query)
  }

  @Test
  fun `geo zero coordinates with no query returns null`() {
    val result = NavigationIntentParser.parseGeoSsp("0,0", null)
    assertNull(result)
  }

  @Test
  fun `geo invalid latitude returns null`() {
    assertNull(NavigationIntentParser.parseGeoSsp("91.0,0.0", null))
  }

  @Test
  fun `geo invalid longitude returns null`() {
    assertNull(NavigationIntentParser.parseGeoSsp("0.0,181.0", null))
  }

  // parseGoogleNavigationSsp

  @Test
  fun `google navigation coordinates`() {
    val result = NavigationIntentParser.parseGoogleNavigationSsp("37.81,-122.42")
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
    assertNull(result.query)
  }

  @Test
  fun `google navigation place name`() {
    val result = NavigationIntentParser.parseGoogleNavigationSsp("Starbucks Seattle")
    assertNotNull(result)
    assertNull(result!!.latitude)
    assertNull(result.longitude)
    assertEquals("Starbucks Seattle", result.query)
  }

  // NavigationDestination display name

  @Test
  fun `display name uses query when available`() {
    val dest = NavigationDestination(37.81, -122.42, "Pier 39")
    assertEquals("Pier 39", dest.displayName)
  }

  @Test
  fun `display name uses coordinates when no query`() {
    val dest = NavigationDestination(37.81, -122.42, null)
    assertEquals("37.8100, -122.4200", dest.displayName)
  }

  @Test
  fun `display name uses unknown when neither`() {
    val dest = NavigationDestination(null, null, null)
    assertEquals("Unknown location", dest.displayName)
  }
}
