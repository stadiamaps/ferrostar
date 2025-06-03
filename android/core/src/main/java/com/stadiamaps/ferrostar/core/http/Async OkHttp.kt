package com.stadiamaps.ferrostar.core.http

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun Call.await(): Response {
    // Lightly adapted from
    // https://github.com/gildor/kotlin-coroutines-okhttp/blob/master/src/main/kotlin/ru/gildor/coroutines/okhttp/CallAwait.kt
    //
    // We copied the implementation because 1) we don't want the old okhttp3 dependency, and 2)
    // we decide to always preserve the call stack even though it's not free as there should be
    // relatively few HTTP requests in this manner.
    val callStack =
        IOException().apply {
            // Remove unnecessary lines from stacktrace
            // This doesn't remove await$default, but better than nothing
            stackTrace = stackTrace.copyOfRange(1, stackTrace.size)
        }

    return suspendCancellableCoroutine { continuation ->
        enqueue(
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response, null)
                }

                override fun onFailure(call: Call, e: IOException) {
                    // Don't bother with resuming the continuation if it is already cancelled.
                    if (continuation.isCancelled) return
                    callStack.initCause(e)
                    continuation.resumeWithException(callStack)
                }
            })

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ex: Throwable) {
                // Ignore cancel exception
            }
        }
    }
}
