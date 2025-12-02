package com.stadiamaps.ferrostar.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteProviderOptionsTest {
  @Test
  fun `serialize a nested map`() {
    val options = mapOf("costing_options" to mapOf("auto" to mapOf("useTolls" to 0)))
    assertEquals("{\"costing_options\":{\"auto\":{\"useTolls\":0}}}", options.toJson())
  }

  @Test
  fun `serialize options with a deeply nested list`() {
    val options =
        mapOf(
            "excluded_polygons" to
                listOf(
                    listOf(
                        listOf(172.258986, -43.454351),
                        listOf(172.242389, -43.485216),
                        listOf(172.302554, -43.495376),
                        listOf(172.322781, -43.458868),
                        listOf(172.258986, -43.454351)),
                    listOf(
                        listOf(172.460225, -43.444937),
                        listOf(172.458669, -43.474302),
                        listOf(172.50068, -43.478442),
                        listOf(172.50846, -43.453974),
                        listOf(172.460225, -43.444937))))
    assertEquals(
        "{\"excluded_polygons\":[[[172.258986,-43.454351],[172.242389,-43.485216],[172.302554,-43.495376],[172.322781,-43.458868],[172.258986,-43.454351]],[[172.460225,-43.444937],[172.458669,-43.474302],[172.50068,-43.478442],[172.50846,-43.453974],[172.460225,-43.444937]]]}",
        options.toJson())
  }

  @Test
  fun `serialize options with a mix of list-like collectons`() {
    val options =
        mapOf(
            "excluded_polygons" to
                arrayOf(
                    arrayOf(
                        arrayListOf(172.258986, -43.454351),
                        arrayListOf(172.242389, -43.485216),
                        arrayListOf(172.302554, -43.495376),
                        arrayListOf(172.322781, -43.458868),
                        arrayListOf(172.258986, -43.454351)),
                    arrayOf(
                        listOf(172.460225, -43.444937),
                        listOf(172.458669, -43.474302),
                        listOf(172.50068, -43.478442),
                        listOf(172.50846, -43.453974),
                        listOf(172.460225, -43.444937))))
    assertEquals(
        "{\"excluded_polygons\":[[[172.258986,-43.454351],[172.242389,-43.485216],[172.302554,-43.495376],[172.322781,-43.458868],[172.258986,-43.454351]],[[172.460225,-43.444937],[172.458669,-43.474302],[172.50068,-43.478442],[172.50846,-43.453974],[172.460225,-43.444937]]]}",
        options.toJson())
  }

  @Test
  fun `serialize options where a list contains a map`() {
    val options = mapOf("foo" to listOf(mapOf("bar" to "baz")))
    assertEquals("{\"foo\":[{\"bar\":\"baz\"}]}", options.toJson())
  }
}
