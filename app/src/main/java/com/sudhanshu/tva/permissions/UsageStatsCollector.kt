package com.sudhanshu.tva.permissions

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

data class AppUsageSummary(
    val packageName: String,
    val totalForegroundMillis: Long
)

/**
 * Reads today's app usage stats — only ever called after confirming
 * PermissionManager.hasUsageStatsPermission == true (Step 7's special
 * Settings-based grant). Summarizes rather than logging every single app
 * switch, to keep this useful without being a moment-by-moment surveillance log.
 */
object UsageStatsCollector {

    fun getTodayUsageSummary(context: Context, topN: Int = 5): List<AppUsageSummary> {
        if (!PermissionManager.hasUsageStatsPermission(context)) return emptyList()

        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime
            ) ?: emptyList()

            stats
                .filter { it.totalTimeInForeground > 0 }
                .sortedByDescending { it.totalTimeInForeground }
                .take(topN)
                .map { AppUsageSummary(it.packageName, it.totalTimeInForeground) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun formatDuration(millis: Long): String {
        val minutes = millis / 60000
        return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
    }
}
