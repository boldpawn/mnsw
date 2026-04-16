package nl.mnsw.visit.infrastructure.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.mnsw.auth.infrastructure.MnswUserDetails
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.infrastructure.persistence.FormalityJpaEntity
import nl.mnsw.visit.application.CreateVisitCommand
import nl.mnsw.visit.application.CreateVisitUseCase
import nl.mnsw.visit.application.GetVisitQuery
import nl.mnsw.visit.application.GetVisitUseCase
import nl.mnsw.visit.infrastructure.persistence.VisitRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

/**
 * REST controller voor havenbezoeken (Visits).
 *
 * POST /api/v1/visits        — visit aanmaken (201 Created)
 * GET  /api/v1/visits        — lijst van visits (200, gepagineerd)
 * GET  /api/v1/visits/{id}   — detail van één visit (200)
 */
@RestController
@RequestMapping("/api/v1/visits")
class VisitController(
    private val createVisitUseCase: CreateVisitUseCase,
    private val getVisitUseCase: GetVisitUseCase,
    private val visitRepository: VisitRepository
) {

    /**
     * POST /api/v1/visits
     * Maakt een nieuw havenbezoek aan. Response: 201 Created.
     */
    @PostMapping
    fun create(
        @RequestBody @Valid request: CreateVisitRequest,
        authentication: Authentication
    ): ResponseEntity<VisitDto> {
        val result = createVisitUseCase.execute(
            CreateVisitCommand(
                imoNumber = request.imoNumber,
                vesselName = request.vesselName,
                vesselFlag = request.vesselFlag,
                portLocode = request.portLocode,
                eta = request.eta,
                etd = request.etd
            )
        )

        // Haal de aangemaakt entity op voor de response
        val visitEntity = visitRepository.findById(result.visitId).orElseThrow()

        return ResponseEntity.status(201).body(
            VisitDto(
                id = visitEntity.id,
                imoNumber = visitEntity.imoNumber,
                vesselName = visitEntity.vesselName,
                vesselFlag = visitEntity.vesselFlag,
                portLocode = visitEntity.portLocode,
                eta = visitEntity.eta,
                etd = visitEntity.etd
            )
        )
    }

    /**
     * GET /api/v1/visits
     * Lijst van visits, gepagineerd en filterbaar.
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) portLocode: String?,
        @RequestParam(required = false) imoNumber: String?,
        @PageableDefault(size = 20) pageable: Pageable,
        authentication: Authentication
    ): ResponseEntity<Page<VisitDto>> {
        val userDetails = authentication.principal as MnswUserDetails

        // Filter op rol: havenautoriteit ziet alleen eigen haven
        val effectivePortLocode = when {
            userDetails.portLocode != null -> userDetails.portLocode
            else -> portLocode
        }

        val page = when {
            effectivePortLocode != null -> visitRepository.findByPortLocode(effectivePortLocode, pageable)
            else -> visitRepository.findAll(pageable)
        }

        val result = if (imoNumber != null) {
            val filtered = page.content.filter { it.imoNumber == imoNumber }
            PageImpl(filtered, pageable, filtered.size.toLong())
        } else {
            page
        }

        return ResponseEntity.ok(result.map { entity ->
            VisitDto(
                id = entity.id,
                imoNumber = entity.imoNumber,
                vesselName = entity.vesselName,
                vesselFlag = entity.vesselFlag,
                portLocode = entity.portLocode,
                eta = entity.eta,
                etd = entity.etd
            )
        })
    }

    /**
     * GET /api/v1/visits/{id}
     * Detailopvraging van één visit inclusief gekoppelde formalities.
     */
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
        authentication: Authentication
    ): ResponseEntity<VisitDetailDto> {
        val result = getVisitUseCase.execute(GetVisitQuery(visitId = id))

        return ResponseEntity.ok(
            VisitDetailDto(
                id = result.visit.id,
                imoNumber = result.visit.imoNumber,
                vesselName = result.visit.vesselName,
                vesselFlag = result.visit.vesselFlag,
                portLocode = result.visit.portLocode,
                eta = result.visit.eta,
                etd = result.visit.etd,
                formalities = result.formalities.map { formality ->
                    FormalitySummaryDto(
                        id = formality.id,
                        type = formality.type,
                        version = formality.version,
                        status = formality.status,
                        submittedAt = formality.submittedAt
                    )
                }
            )
        )
    }
}

data class CreateVisitRequest(
    @field:NotBlank(message = "imoNumber is verplicht")
    val imoNumber: String = "",

    @field:NotBlank(message = "vesselName is verplicht")
    val vesselName: String = "",

    val vesselFlag: String? = null,

    @field:NotBlank(message = "portLocode is verplicht")
    val portLocode: String = "",

    val eta: OffsetDateTime? = null,
    val etd: OffsetDateTime? = null
)

data class VisitDto(
    val id: UUID,
    val imoNumber: String,
    val vesselName: String,
    val vesselFlag: String?,
    val portLocode: String,
    val eta: OffsetDateTime?,
    val etd: OffsetDateTime?
)

data class VisitDetailDto(
    val id: UUID,
    val imoNumber: String,
    val vesselName: String,
    val vesselFlag: String?,
    val portLocode: String,
    val eta: OffsetDateTime?,
    val etd: OffsetDateTime?,
    val formalities: List<FormalitySummaryDto>
)

data class FormalitySummaryDto(
    val id: UUID,
    val type: FormalityType,
    val version: Int,
    val status: FormalityStatus,
    val submittedAt: OffsetDateTime
)
