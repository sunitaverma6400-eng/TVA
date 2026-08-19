package com.sudhanshu.tva.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints exposed by tva-relay. Grows as later steps add features
 * (predictions, temporal search, etc).
 */
interface RelayApi {

    @GET("health")
    suspend fun health(): Response<HealthResponse>

    @GET("api/ping")
    suspend fun ping(): Response<PingResponse>

    // Step 8: Timeline database
    @retrofit2.http.POST("api/timeline/events")
    suspend fun createEvent(@Body event: CreateEventRequest): Response<TimelineEventDto>

    @GET("api/timeline/events")
    suspend fun listEvents(
        @Query("from_time") fromTime: Double? = null,
        @Query("to_time") toTime: Double? = null,
        @Query("limit") limit: Int = 200,
        @Query("branch_id") branchId: String? = null
    ): Response<TimelineEventsResponse>

    @DELETE("api/timeline/events/{eventId}")
    suspend fun deleteEvent(@Path("eventId") eventId: String): Response<Unit>

    @retrofit2.http.PATCH("api/timeline/events/{eventId}/revoke")
    suspend fun revokeEvent(@Path("eventId") eventId: String): Response<TimelineEventDto>

    @retrofit2.http.PATCH("api/timeline/events/{eventId}/unrevoke")
    suspend fun unrevokeEvent(@Path("eventId") eventId: String): Response<TimelineEventDto>

    @GET("api/timeline/events/{eventId}")
    suspend fun getEvent(@Path("eventId") eventId: String): Response<TimelineEventDetailDto>

    @retrofit2.http.POST("api/timeline/events/{eventId}/connect")
    suspend fun connectEvents(
        @Path("eventId") eventId: String,
        @Body body: ConnectEventRequest
    ): Response<Unit>

    @retrofit2.http.PATCH("api/timeline/events/{eventId}/people")
    suspend fun updateEventPeople(
        @Path("eventId") eventId: String,
        @Body body: UpdatePeopleRequest
    ): Response<TimelineEventDto>

    // Step 11: Person & Variant engine
    @retrofit2.http.POST("api/people")
    suspend fun createPerson(@Body body: CreatePersonRequest): Response<PersonDto>

    @GET("api/people")
    suspend fun listPeople(): Response<PeopleListResponse>

    @GET("api/people/{personId}")
    suspend fun getPerson(@Path("personId") personId: String): Response<PersonDetailDto>

    @DELETE("api/people/{personId}")
    suspend fun deletePerson(@Path("personId") personId: String): Response<Unit>

    @retrofit2.http.POST("api/people/{personId}/variants")
    suspend fun createVariant(
        @Path("personId") personId: String,
        @Body body: CreateVariantRequest
    ): Response<VariantDto>

    @DELETE("api/variants/{variantId}")
    suspend fun deleteVariant(@Path("variantId") variantId: String): Response<Unit>

    // Step 12: Multiverse / Branch engine
    @retrofit2.http.POST("api/branches")
    suspend fun createBranch(@Body body: CreateBranchRequest): Response<BranchDto>

    @GET("api/branches")
    suspend fun listBranches(): Response<BranchesListResponse>

    @GET("api/branches/{branchId}")
    suspend fun getBranch(@Path("branchId") branchId: String): Response<BranchDetailDto>

    @DELETE("api/branches/{branchId}")
    suspend fun deleteBranch(@Path("branchId") branchId: String): Response<Unit>

    // Step 13: AI temporal analysis
    @retrofit2.http.POST("api/ai/timeline-analysis")
    suspend fun analyzeTimeline(@Body body: AnalyzeTimelineRequest = AnalyzeTimelineRequest()): Response<StructuredAnalysisDto>

    // Step 14: Future probability engine + Prediction Ledger
    @retrofit2.http.POST("api/ai/predict-future")
    suspend fun predictFuture(@Body body: PredictFutureRequest = PredictFutureRequest()): Response<StructuredAnalysisDto>

    @GET("api/ai/predictions")
    suspend fun listPredictions(@Query("limit") limit: Int = 20, @Query("status") status: String? = null): Response<PredictionsListResponse>

    @retrofit2.http.PATCH("api/ai/predictions/{predictionId}/outcome")
    suspend fun recordPredictionOutcome(
        @Path("predictionId") predictionId: String,
        @Body body: RecordOutcomeRequest
    ): Response<FuturePredictionDto>

    @GET("api/ai/calibration")
    suspend fun getCalibration(): Response<CalibrationDto>

    // Step 15: Temporal anomaly detector
    @retrofit2.http.POST("api/anomalies/scan")
    suspend fun scanAnomalies(): Response<AnomalyScanResponse>

    @GET("api/anomalies")
    suspend fun listAnomalies(@Query("resolved") resolved: String? = null): Response<AnomaliesListResponse>

    @retrofit2.http.PATCH("api/anomalies/{anomalyId}/resolve")
    suspend fun resolveAnomaly(@Path("anomalyId") anomalyId: String): Response<AnomalyDto>

    // Step 16: Temporal search
    @retrofit2.http.POST("api/temporal-search")
    suspend fun temporalSearch(@Body body: TemporalSearchRequest): Response<TemporalSearchResultDto>



    @retrofit2.http.POST("api/device/contacts/sync")
    suspend fun syncContacts(@Body body: ContactsSyncRequest): Response<ContactsSyncResponse>

    @retrofit2.http.POST("api/device/telemetry")
    suspend fun uploadTelemetry(@Body body: TelemetryBatchRequest): Response<TelemetryResponse>

    @retrofit2.http.POST("api/ai/chat")
    suspend fun chat(@Body body: ChatRequest): Response<ChatResponse>

    @GET("api/ai/chat/history")
    suspend fun chatHistory(): Response<ChatHistoryResponse>

    @retrofit2.http.POST("api/ai/vision")
    suspend fun analyzeImage(@Body body: VisionRequest): Response<VisionResponse>

    // Multi-device support (one user, multiple own phones sharing this relay)
    @GET("api/devices")
    suspend fun listDevices(): Response<DevicesListResponse>
}

data class DeviceSummaryDto(
    val device_name: String,
    val event_count: Int,
    val last_synced_at: Double?
)

data class DevicesListResponse(val devices: List<DeviceSummaryDto>)

data class TemporalSearchRequest(val query: String)

data class TemporalSearchResultDto(
    val id: String,
    val query: String,
    val answer: String,
    val provider_used: String?,
    val created_at: Double
)

data class AnomalyDto(
    val id: String,
    val anomaly_type: String,
    val description: String,
    val severity: String,
    val related_event_ids: List<String>,
    val related_person_ids: List<String>,
    val resolved: Boolean,
    val detected_at: Double
)

data class AnomalyScanResponse(
    val new_anomalies: List<AnomalyDto>,
    val total_found_this_scan: Int
)

data class AnomaliesListResponse(val anomalies: List<AnomalyDto>)

data class AnalyzeTimelineRequest(val branch_id: String? = null)
data class PredictFutureRequest(val context: String? = null, val horizon: String? = null)

data class ScenarioDto(
    val label: String,
    val probability: Double,
    val reasoning: String
)

/** Shared shape for both timeline-analysis and predict-future — always keeps
 * directly-supported observations separate from weaker inferences and from
 * speculative scenarios, plus explicit uncertainty and data gaps. */
data class StructuredAnalysisDto(
    val id: String? = null,
    val observations: List<String> = emptyList(),
    val inferences: List<String> = emptyList(),
    val scenarios: List<ScenarioDto> = emptyList(),
    val uncertainty: String = "",
    val data_gaps: List<String> = emptyList(),
    val provider_used: String? = null,
    val event_count: Int? = null,
    val status: String? = null,
    val horizon: String? = null
)

data class FuturePredictionDto(
    val id: String,
    val context_summary: String,
    val scenarios: List<ScenarioDto>,
    val horizon: String?,
    val provider_used: String?,
    val status: String,
    val outcome_evidence_event_ids: List<String>,
    val outcome_notes: String,
    val resolved_at: Double?,
    val created_at: Double
)

data class PredictionsListResponse(val predictions: List<FuturePredictionDto>)

data class RecordOutcomeRequest(
    val status: String,   // supported | partially_supported | not_supported | inconclusive
    val evidence_event_ids: List<String> = emptyList(),
    val notes: String = ""
)

data class CalibrationDto(
    val count: Int,
    val brier_score: Double?,
    val supported_rate: Double?,
    val note: String
)

data class CreateBranchRequest(
    val name: String,
    val origin_event_id: String? = null,
    val description: String = ""
)

data class BranchDto(
    val id: String,
    val name: String,
    val origin_event_id: String?,
    val description: String,
    val created_at: Double
)

data class BranchesListResponse(val branches: List<BranchDto>)

data class BranchDetailDto(
    val id: String,
    val name: String,
    val origin_event_id: String?,
    val description: String,
    val created_at: Double,
    val events: List<TimelineEventDto>,
    val origin_event: TimelineEventDto?
)

data class ConnectEventRequest(val connected_event_id: String)
data class UpdatePeopleRequest(val people: List<String>)

data class TimelineEventDetailDto(
    val id: String,
    val title: String,
    val event_type: String,
    val event_time: Double,
    val people: List<String>,
    val location: String?,
    val source: String,
    val confidence: Double,
    val connected_event_ids: List<String>,
    val connected_events: List<TimelineEventDto>,
    val created_at: Double
)

data class CreatePersonRequest(
    val name: String,
    val relationship: String = "other",
    val notes: String = ""
)

data class PersonDto(
    val id: String,
    val name: String,
    val relationship: String,
    val notes: String,
    val created_at: Double
)

data class PeopleListResponse(val people: List<PersonDto>)

data class PersonDetailDto(
    val id: String,
    val name: String,
    val relationship: String,
    val notes: String,
    val created_at: Double,
    val variants: List<VariantDto>
)

data class CreateVariantRequest(
    val label: String = "Variant",
    val description: String,
    val divergence_point: String = ""
)

data class VariantDto(
    val id: String,
    val person_id: String,
    val label: String,
    val description: String,
    val divergence_point: String,
    val created_at: Double
)

data class HealthResponse(
    val status: String,
    val service: String
)

data class PingResponse(
    val status: String,
    val message: String
)

data class CreateEventRequest(
    val title: String,
    val event_type: String = "general",
    val event_time: Double,           // epoch seconds
    val people: List<String> = emptyList(),
    val location: String? = null,
    val source: String = "manual",
    val confidence: Double = 1.0,
    val connected_event_ids: List<String> = emptyList(),
    val branch_id: String? = null,
    val source_device: String? = null
)

data class TimelineEventDto(
    val id: String,
    val title: String,
    val event_type: String,
    val event_time: Double,
    val people: List<String>,
    val location: String?,
    val source: String,
    val confidence: Double,
    val connected_event_ids: List<String>,
    val branch_id: String?,
    val revoked: Boolean = false,
    val source_device: String? = null,
    val created_at: Double
)

data class TimelineEventsResponse(
    val events: List<TimelineEventDto>,
    val count: Int
)


data class ContactSyncItem(
    val name: String,
    val has_phone_number: Boolean
)

data class ContactsSyncRequest(
    val source_device: String,
    val contacts: List<ContactSyncItem>
)

data class ContactsSyncResponse(
    val synced: Int,
    val device_name: String
)

data class TelemetryItem(
    val kind: String,
    val title: String,
    val event_time: Double,
    val details: Map<String, String> = emptyMap(),
    val location: String? = null,
    val source_device: String
)

data class TelemetryBatchRequest(
    val source_device: String,
    val items: List<TelemetryItem>
)

data class TelemetryResponse(
    val accepted: Int
)

data class ChatRequest(
    val message: String
)

data class ChatResponse(
    val id: String?,
    val answer: String,
    val provider_used: String?,
    val created_at: Double
)

data class ChatHistoryItem(
    val id: String,
    val role: String,
    val content: String,
    val provider_used: String?,
    val created_at: Double
)

data class ChatHistoryResponse(val messages: List<ChatHistoryItem>)

data class VisionRequest(
    val prompt: String,
    val image_base64: String,
    val mime_type: String = "image/jpeg"
)

data class VisionResponse(
    val answer: String,
    val provider_used: String?
)
