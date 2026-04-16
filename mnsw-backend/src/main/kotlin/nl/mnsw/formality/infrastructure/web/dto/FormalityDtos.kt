package nl.mnsw.formality.infrastructure.web.dto

import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

// ===== Request DTOs =====

/**
 * Request DTO voor het indienen van een nieuwe formality.
 * Payload wordt opgeslagen als JsonNode — type-specifieke conversie vindt plaats in FormalityController
 * op basis van het `type` veld in dezelfde request.
 */
data class SubmitFormalityRequest(
    @field:NotNull(message = "visitId is verplicht")
    val visitId: UUID? = null,

    @field:NotNull(message = "type is verplicht")
    val type: FormalityType? = null,

    val lrn: String? = null,

    @field:NotBlank(message = "messageIdentifier is verplicht")
    val messageIdentifier: String = "",

    @field:NotNull(message = "payload is verplicht")
    val payload: JsonNode? = null
)

/**
 * Request DTO voor het indienen van een correctie op een bestaande formality.
 * Type wijzigt niet bij correctie — type-bepaling via originele formality.
 */
data class CorrectFormalityRequest(
    val lrn: String? = null,

    @field:NotBlank(message = "messageIdentifier is verplicht")
    val messageIdentifier: String = "",

    @field:NotNull(message = "payload is verplicht")
    val payload: JsonNode? = null
)

/**
 * Request DTO voor het afwijzen van een formality.
 */
data class RejectFormalityRequest(
    @field:NotBlank(message = "reasonCode is verplicht")
    val reasonCode: String = "",

    @field:NotBlank(message = "reasonDescription is verplicht")
    val reasonDescription: String = ""
)

// ===== Typed Payload Request DTOs =====

/**
 * Gemeenschappelijk type voor alle type-specifieke payload request DTOs.
 * Gebruikt als parameter in FormalityController.mapPayloadRequest().
 */
sealed class FormalityPayloadRequest

/**
 * Type-specifieke payload DTOs voor deserialisatie na type-bepaling.
 * Worden intern gebruikt in FormalityController.mapPayloadRequest().
 */
data class NoaPayloadRequest(
    val expectedArrival: OffsetDateTime? = null,
    val lastPortLocode: String? = null,
    val nextPortLocode: String? = null,
    val purposeOfCall: String? = null,
    val personsOnBoard: Int? = null,
    val dangerousGoods: Boolean = false,
    val wasteDelivery: Boolean = false,
    val maxStaticDraught: BigDecimal? = null
) : FormalityPayloadRequest()

data class NosPayloadRequest(
    val actualSailing: OffsetDateTime? = null,
    val nextPortLocode: String? = null,
    val destinationCountry: String? = null
) : FormalityPayloadRequest()

data class NodPayloadRequest(
    val expectedDeparture: OffsetDateTime? = null,
    val nextPortLocode: String? = null,
    val destinationCountry: String? = null,
    val lastCargoOperations: OffsetDateTime? = null
) : FormalityPayloadRequest()

data class VidPayloadRequest(
    val certificateNationality: String? = null,
    val grossTonnage: BigDecimal? = null,
    val netTonnage: BigDecimal? = null,
    val deadweight: BigDecimal? = null,
    val lengthOverall: BigDecimal? = null,
    val shipType: String? = null,
    val callSign: String? = null,
    val mmsi: String? = null
) : FormalityPayloadRequest()

data class SidPayloadRequest(
    val ispsLevel: Int? = null,
    val last10Ports: List<PortEntryRequest> = emptyList(),
    val securityDeclaration: String? = null,
    val shipToShipActivities: Boolean = false,
    val designatedAuthority: String? = null,
    val ssasActivated: Boolean = false
) : FormalityPayloadRequest()

data class PortEntryRequest(
    val locode: String,
    val arrivalDate: LocalDate? = null,
    val departureDate: LocalDate? = null,
    val ispsLevel: Int? = null
)

// ===== Response DTOs =====

/**
 * Response bij succesvolle indiening van een formality (202 Accepted).
 */
data class SubmitFormalityResultDto(
    val formalityId: UUID,
    val messageIdentifier: String,
    val status: FormalityStatus,
    val statusUrl: String,
    val version: Int? = null,
    val previousVersionId: UUID? = null
)

/**
 * Lijstweergave van een formality voor GET /formalities.
 */
data class FormalityDto(
    val id: UUID,
    val visitId: UUID,
    val type: FormalityType,
    val version: Int,
    val status: FormalityStatus,
    val submitterId: UUID,
    val lrn: String?,
    val messageIdentifier: String,
    val submittedAt: OffsetDateTime,
    val channel: SubmissionChannel
)

/**
 * Detailweergave van een formality voor GET /formalities/{id}.
 * Bevat payload, FRM response en versiehistorie.
 */
data class FormalityDetailDto(
    val id: UUID,
    val visitId: UUID,
    val type: FormalityType,
    val version: Int,
    val status: FormalityStatus,
    val submitterId: UUID,
    val lrn: String?,
    val messageIdentifier: String,
    val submittedAt: OffsetDateTime,
    val channel: SubmissionChannel,
    val payload: Map<String, Any?>,
    val frmResponse: FrmResponseDto?,
    val versionHistory: List<VersionHistoryEntryDto>
)

/**
 * FRM response DTO conform api.md.
 */
data class FrmResponseDto(
    val status: String,
    val reasonCode: String? = null,
    val reasonDescription: String? = null,
    val sentAt: OffsetDateTime?
)

/**
 * Versiehistorie-entry voor GET /formalities/{id}.
 */
data class VersionHistoryEntryDto(
    val id: UUID,
    val version: Int,
    val status: FormalityStatus,
    val submittedAt: OffsetDateTime
)

/**
 * Status response voor PUT /review, /approve, /reject endpoints.
 */
data class FormalityStatusDto(
    val formalityId: UUID,
    val status: FormalityStatus,
    val frmResponse: FrmResponseDto? = null
)
