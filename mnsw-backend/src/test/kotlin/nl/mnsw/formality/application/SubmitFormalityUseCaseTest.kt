package nl.mnsw.formality.application

import nl.mnsw.auth.infrastructure.persistence.UserJpaEntity
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.FormalityValidator
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.formality.domain.payload.FormalityPayload
import nl.mnsw.formality.infrastructure.persistence.FormalityJpaEntity
import nl.mnsw.formality.infrastructure.persistence.FormalityRepository
import nl.mnsw.formality.infrastructure.persistence.NoaPayloadRepository
import nl.mnsw.formality.infrastructure.persistence.NodPayloadRepository
import nl.mnsw.formality.infrastructure.persistence.NosPayloadRepository
import nl.mnsw.formality.infrastructure.persistence.SidPayloadRepository
import nl.mnsw.formality.infrastructure.persistence.VidPayloadRepository
import nl.mnsw.shared.exception.ValidationException
import nl.mnsw.shared.exception.VisitNotFoundException
import nl.mnsw.visit.infrastructure.persistence.VisitJpaEntity
import nl.mnsw.visit.infrastructure.persistence.VisitRepository
import nl.mnsw.auth.infrastructure.persistence.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class SubmitFormalityUseCaseTest {

    private val visitRepository: VisitRepository = mock()
    private val formalityRepository: FormalityRepository = mock()
    private val formalityValidator: FormalityValidator = mock()
    private val noaPayloadRepository: NoaPayloadRepository = mock()
    private val nosPayloadRepository: NosPayloadRepository = mock()
    private val nodPayloadRepository: NodPayloadRepository = mock()
    private val vidPayloadRepository: VidPayloadRepository = mock()
    private val sidPayloadRepository: SidPayloadRepository = mock()
    private val userRepository: UserRepository = mock()
    private val applicationEventPublisher: ApplicationEventPublisher = mock()

    private val useCase = SubmitFormalityUseCase(
        visitRepository = visitRepository,
        formalityRepository = formalityRepository,
        formalityValidator = formalityValidator,
        noaPayloadRepository = noaPayloadRepository,
        nosPayloadRepository = nosPayloadRepository,
        nodPayloadRepository = nodPayloadRepository,
        vidPayloadRepository = vidPayloadRepository,
        sidPayloadRepository = sidPayloadRepository,
        userRepository = userRepository,
        applicationEventPublisher = applicationEventPublisher
    )

    private val visitId = UUID.randomUUID()
    private val submitterId = UUID.randomUUID()
    private val visitEntity = VisitJpaEntity(
        id = visitId,
        imoNumber = "9234567",
        vesselName = "MV Rotterdam",
        portLocode = "NLRTM"
    )
    private val userEntity = UserJpaEntity(
        id = submitterId,
        email = "agent@rederij.nl",
        passwordHash = "hash",
        fullName = "Jan Jansen"
    )
    private val noaPayload = FormalityPayload.NoaPayload(
        expectedArrival = OffsetDateTime.now().plusDays(2),
        lastPortLocode = "GBFXT",
        nextPortLocode = "NLRTM",
        purposeOfCall = "Lossing containers",
        personsOnBoard = 22,
        dangerousGoods = false,
        wasteDelivery = true,
        maxStaticDraught = BigDecimal("11.5")
    )

    @Test
    fun `should submit formality successfully when visit exists and payload is valid`() {
        whenever(visitRepository.findById(visitId)).thenReturn(Optional.of(visitEntity))
        whenever(userRepository.findById(submitterId)).thenReturn(Optional.of(userEntity))
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }
        whenever(noaPayloadRepository.save(any())).thenAnswer { it.arguments[0] }

        val command = SubmitFormalityCommand(
            visitId = visitId,
            type = FormalityType.NOA,
            lrn = "REF-001",
            messageIdentifier = "MSG-001",
            payload = noaPayload,
            submitterId = submitterId,
            channel = SubmissionChannel.WEB
        )

        val result = useCase.execute(command)

        assertNotNull(result.formalityId)
        assertEquals("MSG-001", result.messageIdentifier)
        assertEquals(FormalityStatus.SUBMITTED, result.status)
        verify(formalityRepository).save(any())
        verify(noaPayloadRepository).save(any())
        verify(applicationEventPublisher).publishEvent(any<FormalitySubmittedEvent>())
    }

    @Test
    fun `should throw VisitNotFoundException when visit does not exist`() {
        whenever(visitRepository.findById(visitId)).thenReturn(Optional.empty())

        val command = SubmitFormalityCommand(
            visitId = visitId,
            type = FormalityType.NOA,
            lrn = null,
            messageIdentifier = "MSG-001",
            payload = noaPayload,
            submitterId = submitterId,
            channel = SubmissionChannel.WEB
        )

        assertThrows<VisitNotFoundException> {
            useCase.execute(command)
        }
    }

    @Test
    fun `should throw ValidationException when payload is invalid`() {
        whenever(visitRepository.findById(visitId)).thenReturn(Optional.of(visitEntity))
        doThrow(ValidationException(listOf(nl.mnsw.shared.exception.FieldError(
            "payload.expectedArrival",
            "NOA_ARRIVAL_IN_PAST",
            "Aankomsttijd in verleden"
        )))).whenever(formalityValidator).validate(any(), any(), any(), any())

        val command = SubmitFormalityCommand(
            visitId = visitId,
            type = FormalityType.NOA,
            lrn = null,
            messageIdentifier = "MSG-001",
            payload = noaPayload,
            submitterId = submitterId,
            channel = SubmissionChannel.WEB
        )

        assertThrows<ValidationException> {
            useCase.execute(command)
        }
    }

    @Test
    fun `should publish FormalitySubmittedEvent after successful submission`() {
        whenever(visitRepository.findById(visitId)).thenReturn(Optional.of(visitEntity))
        whenever(userRepository.findById(submitterId)).thenReturn(Optional.of(userEntity))
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }
        whenever(noaPayloadRepository.save(any())).thenAnswer { it.arguments[0] }

        val command = SubmitFormalityCommand(
            visitId = visitId,
            type = FormalityType.NOA,
            lrn = "REF-001",
            messageIdentifier = "MSG-002",
            payload = noaPayload,
            submitterId = submitterId,
            channel = SubmissionChannel.RIM
        )

        useCase.execute(command)

        val eventCaptor = argumentCaptor<FormalitySubmittedEvent>()
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture())
        val event = eventCaptor.firstValue
        assertEquals(visitId, event.visitId)
        assertEquals(FormalityType.NOA, event.type)
        assertEquals("NLRTM", event.portLocode)
        assertEquals(SubmissionChannel.RIM, event.channel)
        assertEquals(submitterId, event.submitterId)
        assertEquals("MSG-002", event.messageIdentifier)
    }

    @Test
    fun `should set status to SUBMITTED on new formality`() {
        whenever(visitRepository.findById(visitId)).thenReturn(Optional.of(visitEntity))
        whenever(userRepository.findById(submitterId)).thenReturn(Optional.of(userEntity))

        val savedEntityCaptor = argumentCaptor<FormalityJpaEntity>()
        whenever(formalityRepository.save(savedEntityCaptor.capture())).thenAnswer { it.arguments[0] as FormalityJpaEntity }
        whenever(noaPayloadRepository.save(any())).thenAnswer { it.arguments[0] }

        val command = SubmitFormalityCommand(
            visitId = visitId,
            type = FormalityType.NOA,
            lrn = null,
            messageIdentifier = "MSG-003",
            payload = noaPayload,
            submitterId = submitterId,
            channel = SubmissionChannel.WEB
        )

        useCase.execute(command)

        val savedEntity = savedEntityCaptor.firstValue
        assertEquals(FormalityStatus.SUBMITTED, savedEntity.status)
        assertEquals(1, savedEntity.version)
        assertEquals(FormalityType.NOA, savedEntity.type)
    }
}
