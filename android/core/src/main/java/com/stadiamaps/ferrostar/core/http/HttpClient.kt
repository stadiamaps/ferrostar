package com.stadiamaps.ferrostar.core.http

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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


class FerrostarOkHttpClient(private val client: OkHttpClient) : IHttpClient {
    override suspend fun call(request: RouteRequest): IResponse {
        val okHttpRequest = request.toOkhttp3Request()

        val res = client.newCall(okHttpRequest).await()

        return res.let { response ->
            object : IResponse {
                override val isSuccessful: Boolean
                    get() = response.isSuccessful

                override val code: Int
                    get() = response.code

                override fun bodyBytes(): ByteArray? = response.body?.bytes()
            }
        }
    }

    companion object {
        fun OkHttpClient.toFerrostarOkHttpClient(): FerrostarOkHttpClient {
            return FerrostarOkHttpClient(this)
        }

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
    }
}