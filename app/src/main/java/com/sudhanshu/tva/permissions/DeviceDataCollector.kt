package com.sudhanshu.tva.permissions

import android.content.Context
import android.provider.CalendarContract
import android.provider.ContactsContract

data class CollectedCalendarEvent(
    val title: String,
    val startTimeEpochMillis: Long,
    val endTimeEpochMillis: Long,
    val location: String?
)

data class CollectedContact(
    val name: String,
    val hasPhoneNumber: Boolean
)

/**
 * Step 7: actual data collection — only ever called after confirming
 * PermissionManager.hasPermission(READ_CALENDAR) == true. This is the
 * foundation Step 8 (Timeline database) will build on: raw device data in,
 * structured timeline events out.
 */
object DeviceDataCollector {

    /**
     * Reads upcoming/recent calendar events. Returns an empty list (never
     * throws to the caller) if permission isn't granted or the query fails
     * — callers should check PermissionManager.hasPermission first anyway.
     */
    fun readCalendarEvents(context: Context, limit: Int = 50): List<CollectedCalendarEvent> {
        if (!PermissionManager.hasPermission(context, android.Manifest.permission.READ_CALENDAR)) {
            return emptyList()
        }

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        )

        val results = mutableListOf<CollectedCalendarEvent>()

        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                null,
                null,
                "${CalendarContract.Events.DTSTART} DESC LIMIT $limit"
            )?.use { cursor ->
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndex(CalendarContract.Events.DTEND)
                val locIdx = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

                while (cursor.moveToNext()) {
                    results.add(
                        CollectedCalendarEvent(
                            title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "" else "",
                            startTimeEpochMillis = if (startIdx >= 0) cursor.getLong(startIdx) else 0L,
                            endTimeEpochMillis = if (endIdx >= 0) cursor.getLong(endIdx) else 0L,
                            location = if (locIdx >= 0) cursor.getString(locIdx) else null
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }

        return results
    }

    /**
     * Reads the device contact list (name + whether they have a phone
     * number) for the user to manually pick from — TVA never auto-imports
     * a contact as a Person. Selection happens in ContactsPickerScreen.
     */
    fun readContacts(context: Context, limit: Int = 500): List<CollectedContact> {
        if (!PermissionManager.hasPermission(context, android.Manifest.permission.READ_CONTACTS)) {
            return emptyList()
        }

        val results = mutableListOf<CollectedContact>()
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                ),
                null,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val hasPhoneIdx = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                while (cursor.moveToNext() && results.size < limit) {
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    if (name.isNullOrBlank()) continue
                    val hasPhone = if (hasPhoneIdx >= 0) cursor.getInt(hasPhoneIdx) > 0 else false
                    results.add(CollectedContact(name, hasPhone))
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }

        return results
    }
}
