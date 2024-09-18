package com.stadiamaps.ferrostar.core

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteRequest
import uniffi.ferrostar.getRoutePolyline

@Throws fun Route.getPolyline(precision: UInt): String = getRoutePolyline(this, precision)

@Throws
fun RouteRequest.toOkhttp3Request(): Request {
  val headers: Map<String, String>
  return when (this) {
        is RouteRequest.HttpPost -> {
          headers = this.headers
          Request.Builder().url(url).post(body.toRequestBody())
        }

        is RouteRequest.HttpGet -> {
          headers = this.headers
          Request.Builder().url(url).get()
        }
      }
      .apply { headers.map { (name, value) -> header(name, value) } }
      .build()
}
