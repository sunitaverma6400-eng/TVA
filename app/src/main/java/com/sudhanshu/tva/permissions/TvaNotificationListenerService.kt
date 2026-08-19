package com.sudhanshu.tva.permissions

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.notificationStatsStore by preferencesDataStore(name = "tva_notification_stats")
private val STATS_KEY = stringPreferencesKey("daily_counts_json")

/**
 * Special-access system service (requires the user to explicitly enable it
 * in Settings > Notification access — same "special permission" pattern as
 * usage stats in Step 7, not a runtime dialog).
 *
 * Deliberately counts ONLY — app package + count per day. Never stores
 * notification title/body text, since that's frequently sensitive (OTPs,
 * private messages) and isn't needed for the "which apps notify you most"
 * pattern this feature is for.
 */
class TvaNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val gson = Gson()
                var current: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()

                applicationContext.notificationStatsStore.edit { store ->
                    val json = store[STATS_KEY]
                    if (!json.isNullOrBlank()) {
                        val type = object : TypeToken<MutableMap<String, MutableMap<String, Int>>>() {}.type
                        current = gson.fromJson(json, type) ?: mutableMapOf()
                    }
                    val todayMap = current.getOrPut(today) { mutableMapOf() }
                    todayMap[packageName] = (todayMap[packageName] ?: 0) + 1
                    store[STATS_KEY] = gson.toJson(current)
                }
            } catch (e: Exception) {
                // Never crash the system notification pipeline over a stats-logging failure.
            }
        }
    }

    companion object {
        fun hasAccess(context: Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ) ?: return false
            return enabledListeners.contains(context.packageName)
        }

        suspend fun getTodayCounts(context: Context): Map<String, Int> {
            val gson = Gson()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            return try {
                val store = context.notificationStatsStore.data.first()
                val json = store[STATS_KEY]
                if (json.isNullOrBlank()) return emptyMap()
                val type = object : TypeToken<Map<String, Map<String, Int>>>() {}.type
                val all: Map<String, Map<String, Int>> = gson.fromJson(json, type) ?: emptyMap()
                all[today] ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}
