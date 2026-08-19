package com.sudhanshu.tva.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sudhanshu.tva.data.DeviceIdentity
import com.sudhanshu.tva.data.ProfileRepository
import com.sudhanshu.tva.network.CreateEventRequest
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.permissions.DeviceDataCollector
import com.sudhanshu.tva.permissions.PermissionManager
import kotlinx.coroutines.flow.first

/**
 * Periodic sync is intentionally narrow: it uploads only the user's own
 * calendar events after the user explicitly enabled Calendar & activity and
 * Android has granted READ_CALENDAR. It does not read camera, microphone,
 * message content, gallery, or other apps' private data.
 */
class ConsentedCalendarSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val profile = ProfileRepository(context).profileFlow.first()

        if (!profile.onboardingComplete || !profile.consentCalendarActivity) {
            return Result.success()
        }
        if (!PermissionManager.hasCalendarPermission(context)) {
            return Result.success()
        }

        val deviceName = DeviceIdentity(context).getDeviceNameOrDefault()
        val events = DeviceDataCollector.readCalendarEvents(context, limit = 100)

        return try {
            for (event in events) {
                RelayClient.api.createEvent(
                    CreateEventRequest(
                        title = event.title.ifBlank { "(untitled calendar event)" },
                        event_type = "calendar",
                        event_time = event.startTimeEpochMillis / 1000.0,
                        location = event.location,
                        source = "calendar",
                        source_device = deviceName
                    )
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
