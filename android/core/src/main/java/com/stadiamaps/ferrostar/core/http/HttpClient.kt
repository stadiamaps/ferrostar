package com.stadiamaps.ferrostar.core.http

import uniffi.ferrostar.RouteRequest


/** Interface representing a basic Response from a HTTP Request */
interface IResponse {
    val isSuccessful: Boolean
    val code: Int

    fun bodyBytes(): ByteArray?
}


/** Interface representing a basic HTTP Client that can make RouteRequests */
interface IHttpClient {
    suspend fun call(request: RouteRequest): IResponse
}

