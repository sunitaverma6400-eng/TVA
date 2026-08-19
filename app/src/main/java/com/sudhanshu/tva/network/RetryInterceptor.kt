package com.sudhanshu.tva.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Step 5: retries transient failures (timeouts, 502/503/429 from a
 * cold-starting or overloaded relay) with exponential backoff, instead of
 * failing the whole request immediately. Does NOT retry 401 (bad secret) or
 * 4xx client errors other than 429 — those won't succeed on retry.
 */
class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null
        var response: Response? = null

        for (attempt in 0..maxRetries) {
            try {
                response?.close()
                response = chain.proceed(request)

                if (response.isSuccessful || !shouldRetry(response.code)) {
                    return response
                }
            } catch (e: IOException) {
                lastException = e
            }

            if (attempt < maxRetries) {
                val backoffMs = (500L * (1 shl attempt)) // 500ms, 1s, 2s
                Thread.sleep(backoffMs)
            }
        }

        response?.let { return it }
        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }

    internal fun shouldRetry(code: Int): Boolean =
        code == 429 || code == 502 || code == 503 || code == 504
}
