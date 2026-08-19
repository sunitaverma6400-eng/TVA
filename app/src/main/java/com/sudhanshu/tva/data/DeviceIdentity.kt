package com.sudhanshu.tva.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.deviceDataStore by preferencesDataStore(name = "tva_device")
private val DEVICE_NAME_KEY = stringPreferencesKey("device_name")

/**
 * Multi-device support for one person's own phones (e.g. Sudhanshu's 3
 * devices, all talking to the same relay). Since every device shares the
 * same RELAY_URL + RELAY_APP_SECRET, they already see the same synced data
 * automatically — this just labels WHICH of the user's own devices a piece
 * of data came from (e.g. "Primary Phone" vs "Backup Phone"), purely for
 * their own reference. Not an identity/auth boundary — all devices are
 * equally trusted since they belong to the same person.
 */
class DeviceIdentity(private val context: Context) {

    val deviceNameFlow: Flow<String?> = context.deviceDataStore.data.map { prefs ->
        prefs[DEVICE_NAME_KEY]
    }

    suspend fun setDeviceName(name: String) {
        context.deviceDataStore.edit { prefs ->
            prefs[DEVICE_NAME_KEY] = name
        }
    }

    suspend fun getDeviceNameOrDefault(): String {
        val stored = deviceNameFlow.first()
        return stored ?: (android.os.Build.MODEL ?: "My Device")
    }
}
