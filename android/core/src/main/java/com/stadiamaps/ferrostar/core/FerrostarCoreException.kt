package com.stadiamaps.ferrostar.core

open class FerrostarCoreException : Exception {
  constructor(message: String) : super(message)

  constructor(message: String, cause: Throwable) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}

class InvalidStatusCodeException(val statusCode: Int) :
    FerrostarCoreException("Route request failed with status code $statusCode")

class NoCachedSession :
    FerrostarCoreException(message = "A resumable cached session snapshot was not found")

class NoResponseBodyException :
    FerrostarCoreException("Route request was successful but had no body bytes")

class UserLocationUnknown :
    FerrostarCoreException(
        "The user location is unknown; ensure the location provider is properly configured")
