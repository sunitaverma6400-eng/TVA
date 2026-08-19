package com.sudhanshu.tva.sync

import android.content.Context

object SyncPreferences {
    private const val PREFS = "tva_sync_preferences"
    private const val KEY_ENABLED = "continuous_sync_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
