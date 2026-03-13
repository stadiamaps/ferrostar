package com.stadiamaps.ferrostar.carapp.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationIntentParserTest {

  // geo: URI tests

  @Test
  fun `geo URI with coordinates`() {
    val result = NavigationIntentParser.parseGeoSsp("37.8100,-122.4200")
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
    assertNull(result.query)
  }

  @Test
  fun `geo URI with query only`() {
    val result = NavigationIntentParser.parseGeoSsp("0,0?q=coffee+shops")
    assertNotNull(result)
    assertNull(result!!.latitude)
    assertNull(result.longitude)
    assertEquals("coffee shops", result.query)
  }

  @Test
  fun `geo URI with coordinates and query`() {
    val result = NavigationIntentParser.parseGeoSsp("37.81,-122.42?q=Pier+39")
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
    assertEquals("Pier 39", result.query)
  }

  @Test
  fun `geo URI with zero coordinates uses query`() {
    val result = NavigationIntentParser.parseGeoSsp("0,0?q=Starbucks")
    assertNotNull(result)
    assertNull(result!!.latitude)
    assertNull(result.longitude)
    assertEquals("Starbucks", result.query)
  }

  @Test
  fun `geo URI with no coordinates and no query returns null`() {
    val result = NavigationIntentParser.parseGeoSsp("0,0")
    assertNull(result)
  }

  // google.navigation: URI tests

  @Test
  fun `google navigation URI with coordinates`() {
    val result = NavigationIntentParser.parseGoogleNavigationSsp("q=37.81,-122.42")
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
    assertNull(result.query)
  }

  @Test
  fun `google navigation URI with place name`() {
    val result = NavigationIntentParser.parseGoogleNavigationSsp("q=Starbucks+Seattle")
    assertNotNull(result)
    assertNull(result!!.latitude)
    assertNull(result.longitude)
    assertEquals("Starbucks Seattle", result.query)
  }

  @Test
  fun `google navigation URI with no q param returns null`() {
    val result = NavigationIntentParser.parseGoogleNavigationSsp("mode=d")
    assertNull(result)
  }

  // Scheme dispatch

  @Test
  fun `unknown scheme returns null`() {
    val result = NavigationIntentParser.parseSchemeSpecificPart("https", "maps.google.com")
    assertNull(result)
  }

  @Test
  fun `geo scheme dispatches correctly`() {
    val result = NavigationIntentParser.parseSchemeSpecificPart("geo", "37.81,-122.42")
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
  }

  // Coordinate validation

  @Test
  fun `invalid latitude returns null`() {
    val result = NavigationIntentParser.parseGeoSsp("91.0,0.0")
    assertNull(result)
  }

  @Test
  fun `invalid longitude returns null`() {
    val result = NavigationIntentParser.parseGeoSsp("0.0,181.0")
    assertNull(result)
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
