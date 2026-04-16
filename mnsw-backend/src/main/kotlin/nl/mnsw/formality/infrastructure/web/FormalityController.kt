package nl.mnsw.formality.infrastructure.web

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Valid
import nl.mnsw.auth.infrastructure.MnswUserDetails
import nl.mnsw.formality.application.ApproveFormalityCommand
import nl.mnsw.formality.application.ApproveFormalityUseCase
import nl.mnsw.formality.application.CorrectFormalityCommand
import nl.mnsw.formality.application.CorrectFormalityUseCase
import nl.mnsw.formality.application.GetFormalityQuery
import nl.mnsw.formality.application.GetFormalityUseCase
import nl.mnsw.formality.application.ListFormalitiesQuery
import nl.mnsw.formality.application.ListFormalitiesUseCase
import nl.mnsw.formality.application.RejectFormalityCommand
import nl.mnsw.formality.application.RejectFormalityUseCase
import nl.mnsw.formality.application.SetUnderReviewCommand
import nl.mnsw.formality.application.SetUnderReviewUseCase
import nl.mnsw.formality.application.SubmitFormalityCommand
import nl.mnsw.formality.application.SubmitFormalityUseCase
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.formality.domain.payload.FormalityPayload
import nl.mnsw.formality.domain.payload.PortEntry
import nl.mnsw.formality.infrastructure.persistence.FrmResponseRepository
import nl.mnsw.formality.infrastructure.web.dto.CorrectFormalityRequest
import nl.mnsw.formality.infrastructure.web.dto.FormalityDetailDto
import nl.mnsw.formality.infrastructure.web.dto.FormalityDto
import nl.mnsw.formality.infrastructure.web.dto.FormalityStatusDto
import nl.mnsw.formality.infrastructure.web.dto.FrmResponseDto
import nl.mnsw.formality.infrastructure.web.dto.NodPayloadRequest
import nl.mnsw.formality.infrastructure.web.dto.NoaPayloadRequest
import nl.mnsw.formality.infrastructure.web.dto.NosPayloadRequest
import nl.mnsw.formality.infrastructure.web.dto.RejectFormalityRequest
import nl.mnsw.formality.infrastructure.web.dto.SidPayloadRequest
import nl.mnsw.formality.infrastructure.web.dto.SubmitFormalityRequest
import nl.mnsw.formality.infrastructure.web.dto.SubmitFormalityResultDto
import nl.mnsw.formality.infrastructure.web.dto.VidPayloadRequest
import nl.mnsw.formality.infrastructure.web.dto.VersionHistoryEntryDto
import nl.mnsw.shared.exception.FieldError
import nl.mnsw.shared.exception.ValidationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * REST controller voor formality CRUD en statusbeheer.
 * Alle endpoints vereisen authenticatie (geconfigureerd in SecurityConfig).
 * Rolgebaseerde autorisatie is geïmplementeerd in de use cases zelf.
 *
 * Payload deserialisatie: het `payload` veld wordt ontvangen als JsonNode en
 * geconverteerd naar het juiste FormalityPayload type op basis van het `type` veld
 * in de request. Dit vermijdt het noodzaak van Jackson polymorfie configuratie.
 */
@RestController
@RequestMapping("/api/v1/formalities")
class FormalityController(
    private val submitFormalityUseCase: SubmitFormalityUseCase,
    private val correctFormalityUseCase: CorrectFormalityUseCase,
    private val approveFormalityUseCase: ApproveFormalityUseCase,
    private val rejectFormalityUseCase: RejectFormalityUseCase,
    private val setUnderReviewUseCase: SetUnderReviewUseCase,
    private val getFormalityUseCase: GetFormalityUseCase,
    private val listFormalitiesUseCase: ListFormalitiesUseCase,
    private val frmResponseRepository: FrmResponseRepository,
    private val formalityMapper: FormalityMapper,
    private val objectMapper: ObjectMapper
) {

    /**
     * POST /api/v1/formalities
     * Dient een nieuwe formality in. Response: 202 Accepted (async verwerking).
     */
    @PostMapping
    fun submit(
        @RequestBody @Valid request: SubmitFormalityRequest,
        authentication: Authentication
    ): ResponseEntity<SubmitFormalityResultDto> {
        val userDetails = authentication.principal as MnswUserDetails
        val payload = mapJsonNodeToPayload(request.type!!, request.payload!!)

        val result = submitFormalityUseCase.execute(
            SubmitFormalityCommand(
                visitId = request.visitId!!,
                type = request.type,
                lrn = request.lrn,
                messageIdentifier = request.messageIdentifier,
                payload = payload,
                submitterId = userDetails.userId,
                channel = SubmissionChannel.WEB
            )
        )

        return ResponseEntity.status(202).body(
            SubmitFormalityResultDto(
                formalityId = result.formalityId,
                messageIdentifier = result.messageIdentifier,
                status = result.status,
                statusUrl = "/api/v1/formalities/${result.formalityId}"
            )
        )
    }

    /**
     * GET /api/v1/formalities
     * Lijst van formalities, gefilterd op rol.
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) type: FormalityType?,
        @RequestParam(required = false) status: FormalityStatus?,
        @RequestParam(required = false) visitId: UUID?,
        @RequestParam(required = false) portLocode: String?,
        @RequestParam(required = false, defaultValue = "false") includeSuperseded: Boolean,
        @PageableDefault(size = 20) pageable: Pageable,
        authentication: Authentication
    ): ResponseEntity<Page<FormalityDto>> {
        val userDetails = authentication.principal as MnswUserDetails

        val formalities = listFormalitiesUseCase.execute(
            ListFormalitiesQuery(
                requestingUserId = userDetails.userId,
                requestingUserRole = userDetails.role,
                requestingUserPortLocode = userDetails.portLocode,
                typeFilter = type,
                statusFilter = status,
                visitIdFilter = visitId,
                includeSuperseded = includeSuperseded,
                pageable = pageable
            )
        )

        return ResponseEntity.ok(formalities.map { formality ->
            FormalityDto(
                id = formality.id,
                visitId = formality.visitId,
                type = formality.type,
                version = formality.version,
                status = formality.status,
                submitterId = formality.submitterId,
                lrn = formality.lrn,
                messageIdentifier = formality.messageIdentifier,
                submittedAt = formality.submittedAt,
                channel = formality.channel
            )
        })
    }

    /**
     * GET /api/v1/formalities/{id}
     * Detailopvraging van één formality inclusief payload en versiehistorie.
     */
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
        authentication: Authentication
    ): ResponseEntity<FormalityDetailDto> {
        val userDetails = authentication.principal as MnswUserDetails

        val result = getFormalityUseCase.execute(
            GetFormalityQuery(
                formalityId = id,
                requestingUserId = userDetails.userId,
                requestingUserRole = userDetails.role,
                requestingUserPortLocode = userDetails.portLocode
            )
        )

        val frmResponse = frmResponseRepository.findByFormalityId(id)?.let { frm ->
            FrmResponseDto(
                status = frm.status.name,
                reasonCode = frm.reasonCode,
                reasonDescription = frm.reasonDescription,
                sentAt = frm.sentAt
            )
        }

        return ResponseEntity.ok(
            FormalityDetailDto(
                id = result.formality.id,
                visitId = result.formality.visitId,
                type = result.formality.type,
                version = result.formality.version,
                status = result.formality.status,
                submitterId = result.formality.submitterId,
                lrn = result.formality.lrn,
                messageIdentifier = result.formality.messageIdentifier,
                submittedAt = result.formality.submittedAt,
                channel = result.formality.channel,
                payload = mapPayloadToMap(result.formality.payload),
                frmResponse = frmResponse,
                versionHistory = result.versionHistory.map { v ->
                    VersionHistoryEntryDto(
                        id = v.id,
                        version = v.version,
                        status = v.status,
                        submittedAt = v.submittedAt
                    )
                }
            )
        )
    }

    /**
     * POST /api/v1/formalities/{id}/corrections
     * Dient een correctie in op een bestaande formality. Response: 202 Accepted.
     */
    @PostMapping("/{id}/corrections")
    fun correct(
        @PathVariable id: UUID,
        @RequestBody @Valid request: CorrectFormalityRequest,
        authentication: Authentication
    ): ResponseEntity<SubmitFormalityResultDto> {
        val userDetails = authentication.principal as MnswUserDetails

        // Haal originele formality op om type te bepalen voor payload-mapping
        val originalResult = getFormalityUseCase.execute(
            GetFormalityQuery(
                formalityId = id,
                requestingUserId = userDetails.userId,
                requestingUserRole = userDetails.role,
                requestingUserPortLocode = userDetails.portLocode
            )
        )

        val payload = mapJsonNodeToPayload(originalResult.formality.type, request.payload!!)

        val result = correctFormalityUseCase.execute(
            CorrectFormalityCommand(
                originalFormalityId = id,
                lrn = request.lrn,
                messageIdentifier = request.messageIdentifier,
                payload = payload,
                submitterId = userDetails.userId
            )
        )

        return ResponseEntity.status(202).body(
            SubmitFormalityResultDto(
                formalityId = result.newFormalityId,
                messageIdentifier = result.messageIdentifier,
                status = result.status,
                statusUrl = "/api/v1/formalities/${result.newFormalityId}",
                version = result.version,
                previousVersionId = result.previousVersionId
            )
        )
    }

    /**
     * PUT /api/v1/formalities/{id}/review
     * Havenautoriteit zet formality op UNDER_REVIEW.
     */
    @PutMapping("/{id}/review")
    fun setUnderReview(
        @PathVariable id: UUID,
        authentication: Authentication
    ): ResponseEntity<FormalityStatusDto> {
        val userDetails = authentication.principal as MnswUserDetails

        val result = setUnderReviewUseCase.execute(
            SetUnderReviewCommand(
                formalityId = id,
                reviewerUserId = userDetails.userId
            )
        )

        return ResponseEntity.ok(
            FormalityStatusDto(
                formalityId = result.formalityId,
                status = result.status
            )
        )
    }

    /**
     * PUT /api/v1/formalities/{id}/approve
     * Havenautoriteit keurt formality goed.
     */
    @PutMapping("/{id}/approve")
    fun approve(
        @PathVariable id: UUID,
        authentication: Authentication
    ): ResponseEntity<FormalityStatusDto> {
        val userDetails = authentication.principal as MnswUserDetails

        val result = approveFormalityUseCase.execute(
            ApproveFormalityCommand(
                formalityId = id,
                reviewerUserId = userDetails.userId
            )
        )

        return ResponseEntity.ok(
            FormalityStatusDto(
                formalityId = result.formalityId,
                status = result.status,
                frmResponse = FrmResponseDto(
                    status = "ACCEPTED",
                    sentAt = result.frmSentAt
                )
            )
        )
    }

    /**
     * PUT /api/v1/formalities/{id}/reject
     * Havenautoriteit wijst formality af.
     */
    @PutMapping("/{id}/reject")
    fun reject(
        @PathVariable id: UUID,
        @RequestBody @Valid request: RejectFormalityRequest,
        authentication: Authentication
    ): ResponseEntity<FormalityStatusDto> {
        val userDetails = authentication.principal as MnswUserDetails

        val result = rejectFormalityUseCase.execute(
            RejectFormalityCommand(
                formalityId = id,
                reasonCode = request.reasonCode,
                reasonDescription = request.reasonDescription,
                reviewerUserId = userDetails.userId
            )
        )

        return ResponseEntity.ok(
            FormalityStatusDto(
                formalityId = result.formalityId,
                status = result.status,
                frmResponse = FrmResponseDto(
                    status = "REJECTED",
                    reasonCode = result.reasonCode,
                    reasonDescription = result.reasonDescription,
                    sentAt = result.frmSentAt
                )
            )
        )
    }

    // ===== Payload mapping hulpfuncties =====

    /**
     * Converteer JsonNode naar het juiste FormalityPayload type op basis van formality type.
     * Gebruikt ObjectMapper om het JsonNode te converteren naar het juiste DTO type.
     */
    private fun mapJsonNodeToPayload(type: FormalityType, payloadNode: JsonNode): FormalityPayload {
        return when (type) {
            FormalityType.NOA -> {
                val req = objectMapper.treeToValue(payloadNode, NoaPayloadRequest::class.java)
                FormalityPayload.NoaPayload(
                    expectedArrival = req.expectedArrival
                        ?: throw ValidationException(listOf(FieldError("payload.expectedArrival", "REQUIRED", "expectedArrival is verplicht voor NOA"))),
                    lastPortLocode = req.lastPortLocode,
                    nextPortLocode = req.nextPortLocode,
                    purposeOfCall = req.purposeOfCall,
                    personsOnBoard = req.personsOnBoard,
                    dangerousGoods = req.dangerousGoods,
                    wasteDelivery = req.wasteDelivery,
                    maxStaticDraught = req.maxStaticDraught
                )
            }
            FormalityType.NOS -> {
                val req = objectMapper.treeToValue(payloadNode, NosPayloadRequest::class.java)
                FormalityPayload.NosPayload(
                    actualSailing = req.actualSailing
                        ?: throw ValidationException(listOf(FieldError("payload.actualSailing", "REQUIRED", "actualSailing is verplicht voor NOS"))),
                    nextPortLocode = req.nextPortLocode,
                    destinationCountry = req.destinationCountry
                )
            }
            FormalityType.NOD -> {
                val req = objectMapper.treeToValue(payloadNode, NodPayloadRequest::class.java)
                FormalityPayload.NodPayload(
                    expectedDeparture = req.expectedDeparture
                        ?: throw ValidationException(listOf(FieldError("payload.expectedDeparture", "REQUIRED", "expectedDeparture is verplicht voor NOD"))),
                    nextPortLocode = req.nextPortLocode,
                    destinationCountry = req.destinationCountry,
                    lastCargoOperations = req.lastCargoOperations
                )
            }
            FormalityType.VID -> {
                val req = objectMapper.treeToValue(payloadNode, VidPayloadRequest::class.java)
                FormalityPayload.VidPayload(
                    certificateNationality = req.certificateNationality,
                    grossTonnage = req.grossTonnage,
                    netTonnage = req.netTonnage,
                    deadweight = req.deadweight,
                    lengthOverall = req.lengthOverall,
                    shipType = req.shipType,
                    callSign = req.callSign,
                    mmsi = req.mmsi
                )
            }
            FormalityType.SID -> {
                val req = objectMapper.treeToValue(payloadNode, SidPayloadRequest::class.java)
                FormalityPayload.SidPayload(
                    ispsLevel = req.ispsLevel
                        ?: throw ValidationException(listOf(FieldError("payload.ispsLevel", "REQUIRED", "ispsLevel is verplicht voor SID"))),
                    last10Ports = req.last10Ports.map { port ->
                        PortEntry(
                            locode = port.locode,
                            arrivalDate = port.arrivalDate,
                            departureDate = port.departureDate,
                            ispsLevel = port.ispsLevel
                        )
                    },
                    securityDeclaration = req.securityDeclaration,
                    shipToShipActivities = req.shipToShipActivities,
                    designatedAuthority = req.designatedAuthority,
                    ssasActivated = req.ssasActivated
                )
            }
        }
    }

    private fun mapPayloadToMap(payload: FormalityPayload): Map<String, Any?> {
        return when (payload) {
            is FormalityPayload.NoaPayload -> mapOf(
                "expectedArrival" to payload.expectedArrival,
                "lastPortLocode" to payload.lastPortLocode,
                "nextPortLocode" to payload.nextPortLocode,
                "purposeOfCall" to payload.purposeOfCall,
                "personsOnBoard" to payload.personsOnBoard,
                "dangerousGoods" to payload.dangerousGoods,
                "wasteDelivery" to payload.wasteDelivery,
                "maxStaticDraught" to payload.maxStaticDraught
            )
            is FormalityPayload.NosPayload -> mapOf(
                "actualSailing" to payload.actualSailing,
                "nextPortLocode" to payload.nextPortLocode,
                "destinationCountry" to payload.destinationCountry
            )
            is FormalityPayload.NodPayload -> mapOf(
                "expectedDeparture" to payload.expectedDeparture,
                "nextPortLocode" to payload.nextPortLocode,
                "destinationCountry" to payload.destinationCountry,
                "lastCargoOperations" to payload.lastCargoOperations
            )
            is FormalityPayload.VidPayload -> mapOf(
                "certificateNationality" to payload.certificateNationality,
                "grossTonnage" to payload.grossTonnage,
                "netTonnage" to payload.netTonnage,
                "deadweight" to payload.deadweight,
                "lengthOverall" to payload.lengthOverall,
                "shipType" to payload.shipType,
                "callSign" to payload.callSign,
                "mmsi" to payload.mmsi
            )
            is FormalityPayload.SidPayload -> mapOf(
                "ispsLevel" to payload.ispsLevel,
                "last10Ports" to payload.last10Ports,
                "securityDeclaration" to payload.securityDeclaration,
                "shipToShipActivities" to payload.shipToShipActivities,
                "designatedAuthority" to payload.designatedAuthority,
                "ssasActivated" to payload.ssasActivated
            )
        }
    }
}
