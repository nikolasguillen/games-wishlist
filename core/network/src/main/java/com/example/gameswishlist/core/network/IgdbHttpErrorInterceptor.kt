package com.example.gameswishlist.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Turns every non-2xx IGDB response into an [IgdbHttpException].
 *
 * Translating here, rather than letting Retrofit throw its own `HttpException`, is what keeps `:core:data`
 * free of a Retrofit dependency while still being able to tell a 404 from a 429. It is a translation, not
 * error handling: nothing is retried, logged or recovered, and the failure still lands in `:core:data`.
 *
 * The error body is not read into the message — on debug builds `HttpLoggingInterceptor` has already
 * dumped it by the time this runs, and on release it would be discarded anyway.
 */
internal class IgdbHttpErrorInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) return response

        // The response is dropped in favour of the exception, so its body has to be released here or the
        // connection stays checked out of the pool.
        response.close()
        throw IgdbHttpException(
            code = response.code,
            message = "HTTP ${response.code} ${response.message}"
        )
    }
}
