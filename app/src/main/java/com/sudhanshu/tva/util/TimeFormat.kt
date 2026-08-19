package com.sudhanshu.tva.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormat {
    private val displayFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun display(epochSeconds: Double): String =
        displayFormat.format(Date((epochSeconds * 1000).toLong()))

    fun day(epochMillis: Long): String =
        dayFormat.format(Date(epochMillis))

    fun nowEpochSeconds(): Double = System.currentTimeMillis() / 1000.0
}
