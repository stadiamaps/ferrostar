package com.stadiamaps.ferrostar.core

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteRequest
import uniffi.ferrostar.getRoutePolyline

@Throws fun Route.getPolyline(precision: UInt): String = getRoutePolyline(this, precision)

@Throws
fun RouteRequest.toOkhttp3Request(): Request =
    when (this) {
      is RouteRequest.HttpPost -> {
        Request.Builder()
            .url(url)
            .post(body.toRequestBody())
            .apply { headers.map { (name, value) -> header(name, value) } }
            .build()
      }
      is RouteRequest.HttpGet -> {
        Request.Builder()
            .url(url)
            .get()
            .apply { headers.map { (name, value) -> header(name, value) } }
            .build()
      }
    }
