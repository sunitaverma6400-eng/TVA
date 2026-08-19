package com.sudhanshu.tva.util

import org.junit.Assert.assertTrue
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `display formats epoch seconds without crashing`() {
        val result = TimeFormat.display(1_700_000_000.0)
        assertTrue(result.isNotBlank())
    }

    @Test
    fun `nowEpochSeconds returns a plausible current timestamp`() {
        val now = TimeFormat.nowEpochSeconds()
        // Sanity range: after 2020-01-01, before 2100-01-01 (catches unit
        // mistakes like accidentally returning millis instead of seconds)
        assertTrue(now > 1_577_836_800.0)
        assertTrue(now < 4_102_444_800.0)
    }

    @Test
    fun `day formats epoch millis without crashing`() {
        val result = TimeFormat.day(1_700_000_000_000L)
        assertTrue(result.isNotBlank())
    }
}
