package nl.mnsw.formality.infrastructure.web

import com.fasterxml.jackson.databind.ObjectMapper
import nl.mnsw.auth.infrastructure.JwtService
import nl.mnsw.auth.infrastructure.MnswUserDetailsService
import nl.mnsw.config.SecurityConfig
import nl.mnsw.formality.application.ApproveFormalityCommand
import nl.mnsw.formality.application.ApproveFormalityResult
import nl.mnsw.formality.application.ApproveFormalityUseCase
import nl.mnsw.formality.application.CorrectFormalityCommand
import nl.mnsw.formality.application.CorrectFormalityResult
import nl.mnsw.formality.application.CorrectFormalityUseCase
import nl.mnsw.formality.application.GetFormalityQuery
import nl.mnsw.formality.application.GetFormalityResult
import nl.mnsw.formality.application.GetFormalityUseCase
import nl.mnsw.formality.application.ListFormalitiesQuery
import nl.mnsw.formality.application.ListFormalitiesUseCase
import nl.mnsw.formality.application.RejectFormalityCommand
import nl.mnsw.formality.application.RejectFormalityResult
import nl.mnsw.formality.application.RejectFormalityUseCase
import nl.mnsw.formality.application.SetUnderReviewCommand
import nl.mnsw.formality.application.SetUnderReviewResult
import nl.mnsw.formality.application.SetUnderReviewUseCase
import nl.mnsw.formality.application.SubmitFormalityCommand
import nl.mnsw.formality.application.SubmitFormalityResult
import nl.mnsw.formality.application.SubmitFormalityUseCase
import nl.mnsw.formality.domain.Formality
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.formality.domain.payload.FormalityPayload
import nl.mnsw.formality.infrastructure.persistence.FrmResponseRepository
import nl.mnsw.shared.exception.UnauthorizedAccessException
import nl.mnsw.support.WithMnswUser
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Integratietests voor FormalityController.
 * Importeert SecurityConfig voor echte beveiligingsconfiguratie.
 * Use cases worden gemocked — geen echte database of Pulsar.
 * @WithMnswUser plaatst de testgebruiker in de Spring Security context.
 */
@WebMvcTest(FormalityController::class)
@Import(SecurityConfig::class)
class FormalityControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var submitFormalityUseCase: SubmitFormalityUseCase

    @MockitoBean
    private lateinit var correctFormalityUseCase: CorrectFormalityUseCase

    @MockitoBean
    private lateinit var approveFormalityUseCase: ApproveFormalityUseCase

    @MockitoBean
    private lateinit var rejectFormalityUseCase: RejectFormalityUseCase

    @MockitoBean
    private lateinit var setUnderReviewUseCase: SetUnderReviewUseCase

    @MockitoBean
    private lateinit var getFormalityUseCase: GetFormalityUseCase

    @MockitoBean
    private lateinit var listFormalitiesUseCase: ListFormalitiesUseCase

    @MockitoBean
    private lateinit var frmResponseRepository: FrmResponseRepository

    @MockitoBean
    private lateinit var formalityMapper: FormalityMapper

    @MockitoBean
    private lateinit var jwtService: JwtService

    @MockitoBean
    private lateinit var userDetailsService: MnswUserDetailsService

    private val agentId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val visitId = UUID.randomUUID()
    private val formalityId = UUID.randomUUID()
    private val now = OffsetDateTime.now()

    private fun buildFormality(submitterId: UUID = agentId): Formality = Formality(
        id = formalityId,
        visitId = visitId,
        type = FormalityType.NOA,
        version = 1,
        status = FormalityStatus.SUBMITTED,
        submitterId = submitterId,
        lrn = "LRN-001",
        messageIdentifier = "MSG-001",
        submittedAt = now,
        supersededBy = null,
        channel = SubmissionChannel.WEB,
        payload = FormalityPayload.NoaPayload(
            expectedArrival = now.plusDays(5),
            lastPortLocode = "GBFXT",
            nextPortLocode = "NLRTM",
            purposeOfCall = "Lossing containers",
            personsOnBoard = 22,
            dangerousGoods = false,
            wasteDelivery = true,
            maxStaticDraught = java.math.BigDecimal("11.5")
        )
    )

    private fun noaSubmitRequest(): Map<String, Any?> = mapOf(
        "visitId" to visitId.toString(),
        "type" to "NOA",
        "lrn" to "LRN-001",
        "messageIdentifier" to "MSG-001",
        "payload" to mapOf(
            "expectedArrival" to now.plusDays(5).toString(),
            "lastPortLocode" to "GBFXT",
            "nextPortLocode" to "NLRTM",
            "purposeOfCall" to "Lossing containers",
            "personsOnBoard" to 22,
            "dangerousGoods" to false,
            "wasteDelivery" to true,
            "maxStaticDraught" to 11.5
        )
    )

    // ===== POST /formalities — happy path =====

    @Test
    @WithMnswUser(userId = "00000000-0000-0000-0000-000000000001", role = "SCHEEPSAGENT")
    fun `POST formalities should return 202 on valid NOA submission`() {
        whenever(submitFormalityUseCase.execute(any<SubmitFormalityCommand>())).thenReturn(
            SubmitFormalityResult(
                formalityId = formalityId,
                messageIdentifier = "MSG-001",
                status = FormalityStatus.SUBMITTED
            )
        )

        mockMvc.post("/api/v1/formalities") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(noaSubmitRequest())
        }.andExpect {
            status { isAccepted() }
            jsonPath("$.formalityId") { value(formalityId.toString()) }
            jsonPath("$.status") { value("SUBMITTED") }
            jsonPath("$.statusUrl") { isString() }
        }
    }

    // ===== POST /formalities — validatiefout =====

    @Test
    @WithMnswUser(userId = "00000000-0000-0000-0000-000000000001", role = "SCHEEPSAGENT")
    fun `POST formalities should return 400 when messageIdentifier is missing`() {
        val invalidRequest = mapOf(
            "visitId" to visitId.toString(),
            "type" to "NOA",
            "payload" to mapOf("expectedArrival" to now.plusDays(5).toString())
        )

        mockMvc.post("/api/v1/formalities") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(invalidRequest)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors") { isArray() }
        }
    }

    // ===== POST /formalities — niet geauthenticeerd =====

    @Test
    fun `POST formalities should return 401 when not authenticated`() {
        mockMvc.post("/api/v1/formalities") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(noaSubmitRequest())
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    // ===== GET /formalities — agent ziet eigen formalities =====

    @Test
    @WithMnswUser(userId = "00000000-0000-0000-0000-000000000001", role = "SCHEEPSAGENT")
    fun `GET formalities should return paginated list for authenticated agent`() {
        val formality = buildFormality(submitterId = agentId)
        whenever(listFormalitiesUseCase.execute(any<ListFormalitiesQuery>()))
            .thenReturn(PageImpl(listOf(formality), PageRequest.of(0, 20), 1))

        mockMvc.get("/api/v1/formalities").andExpect {
            status { isOk() }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content[0].id") { value(formalityId.toString()) }
        }
    }

    // ===== GET /formalities/{id} — agent ziet eigen formality =====

    @Test
    @WithMnswUser(userId = "00000000-0000-0000-0000-000000000001", role = "SCHEEPSAGENT")
    fun `GET formality by id should return detail for owner`() {
        val formality = buildFormality(submitterId = agentId)
        whenever(getFormalityUseCase.execute(any<GetFormalityQuery>()))
            .thenReturn(GetFormalityResult(formality = formality, versionHistory = listOf(formality)))
        whenever(frmResponseRepository.findByFormalityId(formalityId)).thenReturn(null)

        mockMvc.get("/api/v1/formalities/$formalityId").andExpect {
            status { isOk() }
            jsonPath("$.id") { value(formalityId.toString()) }
            jsonPath("$.type") { value("NOA") }
            jsonPath("$.versionHistory") { isArray() }
        }
    }

    // ===== GET /formalities/{id} — 403 als niet eigenaar =====

    @Test
    @WithMnswUser(userId = "00000000-0000-0000-0000-000000000002", role = "SCHEEPSAGENT")
    fun `GET formality by id should return 403 when agent is not owner`() {
        whenever(getFormalityUseCase.execute(any<GetFormalityQuery>()))
            .thenThrow(UnauthorizedAccessException("Geen toegang"))

        mockMvc.get("/api/v1/formalities/$formalityId").andExpect {
            status { isForbidden() }
            jsonPath("$.errors[0].code") { value("FORBIDDEN") }
        }
    }

    // ===== POST /formalities/{id}/corrections — niet eigenaar =====

    @Test
    @WithMnswUser(userId = "00000000-0000-0000-0000-000000000002", role = "SCHEEPSAGENT")
    fun `POST corrections should return 403 when not original submitter`() {
        val formality = buildFormality(submitterId = agentId)
        whenever(getFormalityUseCase.execute(any<GetFormalityQuery>()))
            .thenReturn(GetFormalityResult(formality = formality, versionHistory = listOf(formality)))
        whenever(correctFormalityUseCase.execute(any<CorrectFormalityCommand>()))
            .thenThrow(UnauthorizedAccessException("Alleen de originele indiener mag corrigeren"))

        val correctionRequest = mapOf(
            "lrn" to "LRN-002",
            "messageIdentifier" to "MSG-002",
            "payload" to mapOf(
                "expectedArrival" to now.plusDays(6).toString(),
                "dangerousGoods" to false,
                "wasteDelivery" to false
            )
        )

        mockMvc.post("/api/v1/formalities/$formalityId/corrections") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(correctionRequest)
        }.andExpect {
            status { isForbidden() }
        }
    }

    // ===== PUT /formalities/{id}/approve — HAVENAUTORITEIT =====

    @Test
    @WithMnswUser(userId = "00000000-0000-0000-0000-000000000003", role = "HAVENAUTORITEIT", portLocode = "NLRTM")
    fun `PUT approve should return 200 for HAVENAUTORITEIT`() {
        whenever(approveFormalityUseCase.execute(any<ApproveFormalityCommand>())).thenReturn(
            ApproveFormalityResult(
                formalityId = formalityId,
                status = FormalityStatus.ACCEPTED,
                frmSentAt = now
            )
        )

        mockMvc.put("/api/v1/formalities/$formalityId/approve").andExpect {
            status { isOk() }
            jsonPath("$.formalityId") { value(formalityId.toString()) }
            jsonPath("$.status") { value("ACCEPTED") }
        }
    }

    @Test
    @WithMnswUser(userId = "00000000-0000-0000-0000-000000000001", role = "SCHEEPSAGENT")
    fun `PUT approve should return 403 when SCHEEPSAGENT tries to approve`() {
        whenever(approveFormalityUseCase.execute(any<ApproveFormalityCommand>()))
            .thenThrow(UnauthorizedAccessException("Geen havenautoriteit toegang"))

        mockMvc.put("/api/v1/formalities/$formalityId/approve").andExpect {
            status { isForbidden() }
        }
    }

    // ===== PUT /formalities/{id}/review =====

    @Test
    @WithMnswUser(userId = "00000000-0000-0000-0000-000000000003", role = "HAVENAUTORITEIT", portLocode = "NLRTM")
    fun `PUT review should return 200 for HAVENAUTORITEIT`() {
        whenever(setUnderReviewUseCase.execute(any<SetUnderReviewCommand>())).thenReturn(
            SetUnderReviewResult(
                formalityId = formalityId,
                status = FormalityStatus.UNDER_REVIEW
            )
        )

        mockMvc.put("/api/v1/formalities/$formalityId/review").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UNDER_REVIEW") }
        }
    }

    // ===== PUT /formalities/{id}/reject =====

    @Test
    @WithMnswUser(userId = "00000000-0000-0000-0000-000000000003", role = "HAVENAUTORITEIT", portLocode = "NLRTM")
    fun `PUT reject should return 200 with frmResponse for HAVENAUTORITEIT`() {
        whenever(rejectFormalityUseCase.execute(any<RejectFormalityCommand>())).thenReturn(
            RejectFormalityResult(
                formalityId = formalityId,
                status = FormalityStatus.REJECTED,
                reasonCode = "INVALID_IMO",
                reasonDescription = "Het IMO-nummer is ongeldig",
                frmSentAt = now
            )
        )

        val rejectRequest = mapOf(
            "reasonCode" to "INVALID_IMO",
            "reasonDescription" to "Het IMO-nummer is ongeldig"
        )

        mockMvc.put("/api/v1/formalities/$formalityId/reject") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(rejectRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("REJECTED") }
            jsonPath("$.frmResponse.reasonCode") { value("INVALID_IMO") }
        }
    }
}
