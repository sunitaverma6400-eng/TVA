package com.sudhanshu.tva.data

/**
 * Step 6: Personal Identity Engine.
 *
 * Every data category the app is ALLOWED to use is gated behind its own
 * explicit consent flag. All flags default to false — nothing is collected
 * until the user actively agrees to that specific category in ConsentScreen.
 *
 * This only ever represents the SIGNED-IN USER's own data. TVA does not
 * access any other person's private data — "People" (Step 11 variants,
 * relationships) are entries the user manually adds about others, never
 * pulled from another person's device/accounts.
 */
data class UserProfile(
    val name: String = "",
    val dateOfBirth: String = "",   // ISO yyyy-MM-dd, user-entered
    val bio: String = "",

    // Consent flags — Step 7 (permission/data collection layer) will only
    // request the corresponding Android permission if its flag here is true.
    val consentBasicProfile: Boolean = false,      // name/DOB/bio you type in
    val consentDeviceSignals: Boolean = false,      // app usage stats, notifications
    val consentCalendarActivity: Boolean = false,   // calendar/contacts/location
    val consentHistoricalImport: Boolean = false,   // manually imported files/data
    val consentContinuousSync: Boolean = false,     // visible foreground-service sync of consented device signals

    val onboardingComplete: Boolean = false,
    val profileCreatedAtEpochMillis: Long = 0L
) {
    val hasAnyDataConsent: Boolean
        get() = consentBasicProfile || consentDeviceSignals || consentCalendarActivity || consentHistoricalImport || consentContinuousSync
}
