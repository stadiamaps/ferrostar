package com.stadiamaps.ferrostar.car.app

import com.stadiamaps.ferrostar.car.app.intent.NavigationDestination
import com.stadiamaps.ferrostar.car.app.intent.NavigationIntentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationIntentParserTest {

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

  // parseSchemeSpecificPart

  @Test
  fun `ssp geo coordinates`() {
    val result = NavigationIntentParser.parseSchemeSpecificPart("geo", "37.81,-122.42")
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
    assertNull(result.query)
  }

  @Test
  fun `ssp geo coordinates with zoom`() {
    val result = NavigationIntentParser.parseSchemeSpecificPart("geo", "37.81,-122.42?z=11")
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertNull(result.query)
  }

  @Test
  fun `ssp geo encoded free text query`() {
    val result = NavigationIntentParser.parseSchemeSpecificPart("geo", "0,0?q=coffee+shops%2C+SF")
    assertNotNull(result)
    assertNull(result!!.latitude)
    assertEquals("coffee shops, SF", result.query)
  }

  @Test
  fun `ssp geo query among other parameters`() {
    val result = NavigationIntentParser.parseSchemeSpecificPart("geo", "0,0?z=11&q=Pier+39")
    assertEquals("Pier 39", result!!.query)
  }

  @Test
  fun `ssp geo labeled coordinates in query`() {
    val result =
        NavigationIntentParser.parseSchemeSpecificPart("geo", "0,0?q=37.81,-122.42(Pier+39)")
    assertNotNull(result)
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
    assertEquals("Pier 39", result.query)
  }

  @Test
  fun `ssp geo coordinates in query without label`() {
    val result = NavigationIntentParser.parseSchemeSpecificPart("geo", "0,0?q=37.81,-122.42")
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertNull(result.query)
  }

  @Test
  fun `ssp geo zero coordinates without query returns null`() {
    assertNull(NavigationIntentParser.parseSchemeSpecificPart("geo", "0,0"))
    assertNull(NavigationIntentParser.parseSchemeSpecificPart("geo", "0,0?z=11"))
  }

  @Test
  fun `ssp google navigation coordinates`() {
    val result =
        NavigationIntentParser.parseSchemeSpecificPart("google.navigation", "q=37.81,-122.42")
    assertEquals(37.81, result!!.latitude!!, 0.0001)
    assertEquals(-122.42, result.longitude!!, 0.0001)
    assertNull(result.query)
  }

  @Test
  fun `ssp google navigation encoded place name`() {
    val result =
        NavigationIntentParser.parseSchemeSpecificPart("google.navigation", "q=Starbucks+Seattle")
    assertNull(result!!.latitude)
    assertEquals("Starbucks Seattle", result.query)
  }

  @Test
  fun `ssp google navigation without q returns null`() {
    assertNull(NavigationIntentParser.parseSchemeSpecificPart("google.navigation", "mode=d"))
  }

  @Test
  fun `ssp unknown scheme returns null`() {
    assertNull(NavigationIntentParser.parseSchemeSpecificPart("https", "//example.com"))
    assertNull(NavigationIntentParser.parseSchemeSpecificPart(null, null))
  }

  @Test
  fun `ssp malformed percent encoding is kept as is`() {
    val result = NavigationIntentParser.parseSchemeSpecificPart("geo", "0,0?q=100%25%zz")
    assertEquals("100%25%zz", result!!.query)
  }

  // parseLabeledCoordinates

  @Test
  fun `labeled coordinates out of range returns null`() {
    assertNull(NavigationIntentParser.parseLabeledCoordinates("91,0(Nowhere)"))
  }

  @Test
  fun `labeled coordinates with empty label has no query`() {
    val result = NavigationIntentParser.parseLabeledCoordinates("37.81,-122.42()")
    assertNull(result!!.query)
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
