# TVA Android App v1.4

Kotlin + Jetpack Compose companion app for the TVA relay.

## v1.4 fixes

- Debug network logging is now BASIC instead of BODY so timeline/location/person data is not dumped into Logcat.
- `RELAY_URL` must be HTTPS and cannot remain the placeholder host.
- GitHub Actions installs a pinned Gradle 8.7 directly; it no longer depends on generating a missing wrapper at runtime.
- Added WorkManager-based periodic calendar sync every 6 hours, only when the user explicitly consented to Calendar & activity and Android granted `READ_CALENDAR`.
- Background sync uploads calendar data only; it does not collect camera, microphone, gallery or notification message content.
- Relay-side calendar deduplication prevents repeated background syncs from creating duplicate events.
- Permission UI now reports Calendar, Contacts and Location separately instead of treating any single granted permission as the whole category.
- Foreground location logging requests a fresh location fix first and falls back to the newest last-known fix if necessary.
- Notification listener continues to store only app package/count statistics, never notification text.
- Version bumped to 1.4.0.

## GitHub Actions secrets

Set these repository Actions secrets:

| Secret | Purpose |
|---|---|
| `RELAY_URL` | HTTPS URL of the deployed TVA relay |
| `RELAY_APP_SECRET` | Same server secret configured on Render |
| `RELEASE_KEYSTORE_BASE64` | Optional release keystore |
| `RELEASE_KEYSTORE_PASSWORD` | Optional keystore password |
| `RELEASE_KEY_ALIAS` | Optional release alias |
| `RELEASE_KEY_PASSWORD` | Optional alias password |

The workflow produces both debug and release APK artifacts.

## Important security note

`RELAY_APP_SECRET` is a build-time client credential, not a perfect device-authentication boundary: any credential embedded in an APK can potentially be extracted. The relay still keeps Gemini/Groq provider keys server-side. For a multi-user public deployment, replace the shared client secret with per-device enrollment/token authentication before distributing the APK broadly.


## v1.5 feature set

This build adds the missing device features from the TVA roadmap, all opt-in:

- Contacts sync: explicit READ_CONTACTS permission; syncs display name + phone-presence to relay.
- Usage insights: Android Usage Stats summary (top apps + foreground minutes).
- Notification insights: package-level daily notification counts only; notification text/body is never stored.
- Continuous device sync: visible foreground service, ~15-minute cycle, persistent notification, only consented categories.
- Continuous location tracking: separate visible location foreground service, ~5-minute samples, persistent notification, user-controlled start/stop.
- Camera Vision Mode: user-triggered camera capture sent to Gemini multimodal analysis; image is not stored by the relay.
- AI Chat: persistent conversation history plus recent TVA timeline context.
- Existing calendar sync remains idempotent and runs with WorkManager.
