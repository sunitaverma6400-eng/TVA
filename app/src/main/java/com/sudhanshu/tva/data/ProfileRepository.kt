package com.sudhanshu.tva.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "tva_profile")
private val PROFILE_KEY = stringPreferencesKey("user_profile_json")

/**
 * Local-only persistence for the user's own profile. Nothing here syncs to
 * the relay yet — that comes in a later step once the timeline/people
 * database exists server-side (Step 8/13). Until then this is entirely
 * on-device, which is the safer default anyway for identity data.
 */
class ProfileRepository(private val context: Context) {

    private val gson = Gson()

    val profileFlow: Flow<UserProfile> = context.profileDataStore.data.map { prefs ->
        val json = prefs[PROFILE_KEY]
        if (json.isNullOrBlank()) {
            UserProfile()
        } else {
            try {
                gson.fromJson(json, UserProfile::class.java) ?: UserProfile()
            } catch (e: Exception) {
                UserProfile() // corrupted data — fall back to a fresh, unconsented profile
            }
        }
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[PROFILE_KEY] = gson.toJson(profile)
        }
    }

    suspend fun clearProfile() {
        context.profileDataStore.edit { prefs ->
            prefs.remove(PROFILE_KEY)
        }
    }
}
