package com.sudhanshu.tva.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryInterceptorTest {

    private val interceptor = RetryInterceptor()

    @Test
    fun `retries transient server errors and rate limits`() {
        assertTrue(interceptor.shouldRetry(429))
        assertTrue(interceptor.shouldRetry(502))
        assertTrue(interceptor.shouldRetry(503))
        assertTrue(interceptor.shouldRetry(504))
    }

    @Test
    fun `does not retry client errors that will not fix themselves`() {
        assertFalse(interceptor.shouldRetry(401)) // bad secret — retrying won't help
        assertFalse(interceptor.shouldRetry(400))
        assertFalse(interceptor.shouldRetry(404))
    }

    @Test
    fun `does not retry success codes`() {
        assertFalse(interceptor.shouldRetry(200))
        assertFalse(interceptor.shouldRetry(201))
    }
}
