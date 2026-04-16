package nl.mnsw.formality.infrastructure.messaging

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import nl.mnsw.formality.application.SubmitFormalityCommand
import nl.mnsw.formality.application.SubmitFormalityResult
import nl.mnsw.formality.application.SubmitFormalityUseCase
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.shared.exception.FieldError
import nl.mnsw.shared.exception.ValidationException
import nl.mnsw.visit.infrastructure.persistence.VisitJpaEntity
import nl.mnsw.visit.infrastructure.persistence.VisitRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class RimPulsarConsumerTest {

    private val submitFormalityUseCase: SubmitFormalityUseCase = mock()
    private val visitRepository: VisitRepository = mock()
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    private val rimSystemUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private val consumer = RimPulsarConsumer(
        submitFormalityUseCase = submitFormalityUseCase,
        visitRepository = visitRepository,
        objectMapper = objectMapper,
        rimSystemUserId = rimSystemUserId
    )

    private val visitId = UUID.randomUUID()
    private val formalityId = UUID.randomUUID()

    private val visitEntity = VisitJpaEntity(
        id = visitId,
        imoNumber = "9234567",
        vesselName = "MV Rotterdam",
        portLocode = "NLRTM"
    )

    private fun buildNoaRimMessage(): String {
        val message = RimInboundMessage(
            messageIdentifier = "MSG-RIM-001",
            lrn = "LRN-001",
            senderEori = "NL123456789",
            visitInfo = RimVisitInfo(
                imoNumber = "9234567",
                vesselName = "MV Rotterdam",
                portLocode = "NLRTM",
                eta = "2025-12-05T08:00:00Z",
                etd = "2025-12-10T16:00:00Z"
            ),
            payload = mapOf(
                "expectedArrival" to "2025-12-05T08:00:00Z",
                "lastPortLocode" to "GBFXT",
                "nextPortLocode" to "NLRTM",
                "purposeOfCall" to "Lossing containers",
                "personsOnBoard" to 22,
                "dangerousGoods" to false,
                "wasteDelivery" to true,
                "maxStaticDraught" to 11.5
            ),
            receivedAt = "2025-12-01T10:00:00Z"
        )
        return objectMapper.writeValueAsString(message)
    }

    private fun mockSuccessfulSubmit(): SubmitFormalityResult {
        val result = SubmitFormalityResult(
            formalityId = formalityId,
            messageIdentifier = "MSG-RIM-001",
            status = FormalityStatus.SUBMITTED
        )
        whenever(submitFormalityUseCase.execute(any())).thenReturn(result)
        return result
    }

    @Test
    fun `should call SubmitFormalityUseCase with correct type for NOA message`() {
        whenever(visitRepository.findByImoNumber("9234567")).thenReturn(listOf(visitEntity))
        mockSuccessfulSubmit()

        consumer.consumeNoa(buildNoaRimMessage())

        val commandCaptor = argumentCaptor<SubmitFormalityCommand>()
        verify(submitFormalityUseCase).execute(commandCaptor.capture())
        assertEquals(FormalityType.NOA, commandCaptor.firstValue.type)
        assertEquals(SubmissionChannel.RIM, commandCaptor.firstValue.channel)
        assertEquals("MSG-RIM-001", commandCaptor.firstValue.messageIdentifier)
        assertEquals("LRN-001", commandCaptor.firstValue.lrn)
    }

    @Test
    fun `should use existing visit when found by imoNumber and portLocode`() {
        whenever(visitRepository.findByImoNumber("9234567")).thenReturn(listOf(visitEntity))
        mockSuccessfulSubmit()

        consumer.consumeNoa(buildNoaRimMessage())

        verify(visitRepository, never()).save(any())
        val commandCaptor = argumentCaptor<SubmitFormalityCommand>()
        verify(submitFormalityUseCase).execute(commandCaptor.capture())
        assertEquals(visitId, commandCaptor.firstValue.visitId)
    }

    @Test
    fun `should create new visit when no existing visit found`() {
        whenever(visitRepository.findByImoNumber("9234567")).thenReturn(emptyList())
        whenever(visitRepository.save(any())).thenReturn(visitEntity)
        mockSuccessfulSubmit()

        consumer.consumeNoa(buildNoaRimMessage())

        verify(visitRepository).save(any())
    }

    @Test
    fun `should use rimSystemUserId as submitterId`() {
        whenever(visitRepository.findByImoNumber("9234567")).thenReturn(listOf(visitEntity))
        mockSuccessfulSubmit()

        consumer.consumeNoa(buildNoaRimMessage())

        val commandCaptor = argumentCaptor<SubmitFormalityCommand>()
        verify(submitFormalityUseCase).execute(commandCaptor.capture())
        assertEquals(rimSystemUserId, commandCaptor.firstValue.submitterId)
    }

    @Test
    fun `should swallow ValidationException and not rethrow`() {
        whenever(visitRepository.findByImoNumber("9234567")).thenReturn(listOf(visitEntity))
        doThrow(ValidationException(listOf(FieldError("expectedArrival", "ARRIVAL_IN_PAST", "Aankomst in verleden"))))
            .whenever(submitFormalityUseCase).execute(any())

        // Geen exception — bericht wordt acknowledged (niet opnieuw verwerkt)
        consumer.consumeNoa(buildNoaRimMessage())
    }

    @Test
    fun `should rethrow technical exception for negative-acknowledge`() {
        whenever(visitRepository.findByImoNumber("9234567")).thenReturn(listOf(visitEntity))
        doThrow(RuntimeException("Database connection lost"))
            .whenever(submitFormalityUseCase).execute(any())

        // Technische fout wordt opnieuw gegooid — Spring Pulsar negative-acknowledges voor retry
        assertThrows<RuntimeException> {
            consumer.consumeNoa(buildNoaRimMessage())
        }
    }

    @Test
    fun `should call SubmitFormalityUseCase with NOS type for NOS topic`() {
        val nosMessage = RimInboundMessage(
            messageIdentifier = "MSG-NOS-001",
            lrn = null,
            senderEori = "NL123456789",
            visitInfo = RimVisitInfo("9234567", "MV Rotterdam", "NLRTM", null, null),
            payload = mapOf("actualSailing" to "2025-12-10T16:00:00Z"),
            receivedAt = "2025-12-10T15:00:00Z"
        )
        whenever(visitRepository.findByImoNumber("9234567")).thenReturn(listOf(visitEntity))
        mockSuccessfulSubmit()

        consumer.consumeNos(objectMapper.writeValueAsString(nosMessage))

        val commandCaptor = argumentCaptor<SubmitFormalityCommand>()
        verify(submitFormalityUseCase).execute(commandCaptor.capture())
        assertEquals(FormalityType.NOS, commandCaptor.firstValue.type)
    }
}
