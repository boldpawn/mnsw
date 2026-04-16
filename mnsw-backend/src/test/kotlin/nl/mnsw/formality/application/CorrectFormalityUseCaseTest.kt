package nl.mnsw.formality.application

import nl.mnsw.auth.domain.Role
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
import nl.mnsw.shared.exception.ConcurrentCorrectionException
import nl.mnsw.shared.exception.FormalityNotFoundException
import nl.mnsw.shared.exception.UnauthorizedAccessException
import nl.mnsw.visit.infrastructure.persistence.VisitJpaEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class CorrectFormalityUseCaseTest {

    private val formalityRepository: FormalityRepository = mock()
    private val formalityValidator: FormalityValidator = mock()
    private val noaPayloadRepository: NoaPayloadRepository = mock()
    private val nosPayloadRepository: NosPayloadRepository = mock()
    private val nodPayloadRepository: NodPayloadRepository = mock()
    private val vidPayloadRepository: VidPayloadRepository = mock()
    private val sidPayloadRepository: SidPayloadRepository = mock()
    private val applicationEventPublisher: ApplicationEventPublisher = mock()

    private val useCase = CorrectFormalityUseCase(
        formalityRepository = formalityRepository,
        formalityValidator = formalityValidator,
        noaPayloadRepository = noaPayloadRepository,
        nosPayloadRepository = nosPayloadRepository,
        nodPayloadRepository = nodPayloadRepository,
        vidPayloadRepository = vidPayloadRepository,
        sidPayloadRepository = sidPayloadRepository,
        applicationEventPublisher = applicationEventPublisher
    )

    private val submitterId = UUID.randomUUID()
    private val originalFormalityId = UUID.randomUUID()
    private val visitId = UUID.randomUUID()

    private val visitEntity = VisitJpaEntity(
        id = visitId,
        imoNumber = "9234567",
        vesselName = "MV Rotterdam",
        portLocode = "NLRTM"
    )
    private val submitterEntity = UserJpaEntity(
        id = submitterId,
        email = "agent@rederij.nl",
        passwordHash = "hash",
        fullName = "Jan Jansen",
        role = Role.SCHEEPSAGENT
    )
    private val originalFormality = FormalityJpaEntity(
        id = originalFormalityId,
        visit = visitEntity,
        type = FormalityType.NOA,
        version = 1,
        status = FormalityStatus.SUBMITTED,
        submitter = submitterEntity,
        lrn = "REF-001",
        messageIdentifier = "MSG-001",
        channel = SubmissionChannel.WEB
    )
    private val correctedPayload = FormalityPayload.NoaPayload(
        expectedArrival = OffsetDateTime.now().plusDays(3),
        lastPortLocode = "DEHAM",
        nextPortLocode = "NLRTM",
        purposeOfCall = "Gecorrigeerde aankomst",
        personsOnBoard = 25,
        dangerousGoods = false,
        wasteDelivery = false,
        maxStaticDraught = BigDecimal("12.0")
    )

    @Test
    fun `should create new version when correction is valid`() {
        whenever(formalityRepository.findById(originalFormalityId)).thenReturn(Optional.of(originalFormality))
        whenever(formalityRepository.findCurrentVersionByVisitIdAndType(visitId, FormalityType.NOA))
            .thenReturn(originalFormality)
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }
        whenever(noaPayloadRepository.save(any())).thenAnswer { it.arguments[0] }

        val command = CorrectFormalityCommand(
            originalFormalityId = originalFormalityId,
            lrn = "REF-002",
            messageIdentifier = "MSG-002",
            payload = correctedPayload,
            submitterId = submitterId
        )

        val result = useCase.execute(command)

        assertNotNull(result.newFormalityId)
        assertEquals(2, result.version)
        assertEquals(FormalityStatus.SUBMITTED, result.status)
        assertEquals(originalFormalityId, result.previousVersionId)
    }

    @Test
    fun `should set original formality to SUPERSEDED after correction`() {
        whenever(formalityRepository.findById(originalFormalityId)).thenReturn(Optional.of(originalFormality))
        whenever(formalityRepository.findCurrentVersionByVisitIdAndType(visitId, FormalityType.NOA))
            .thenReturn(originalFormality)
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }
        whenever(noaPayloadRepository.save(any())).thenAnswer { it.arguments[0] }

        val command = CorrectFormalityCommand(
            originalFormalityId = originalFormalityId,
            lrn = "REF-002",
            messageIdentifier = "MSG-002",
            payload = correctedPayload,
            submitterId = submitterId
        )

        useCase.execute(command)

        assertEquals(FormalityStatus.SUPERSEDED, originalFormality.status)
        assertNotNull(originalFormality.supersededBy)
    }

    @Test
    fun `should throw UnauthorizedAccessException when different submitter tries to correct`() {
        val differentSubmitterId = UUID.randomUUID()
        whenever(formalityRepository.findById(originalFormalityId)).thenReturn(Optional.of(originalFormality))

        val command = CorrectFormalityCommand(
            originalFormalityId = originalFormalityId,
            lrn = "REF-002",
            messageIdentifier = "MSG-002",
            payload = correctedPayload,
            submitterId = differentSubmitterId  // Andere indiener
        )

        assertThrows<UnauthorizedAccessException> {
            useCase.execute(command)
        }
    }

    @Test
    fun `should throw FormalityNotFoundException when original formality does not exist`() {
        whenever(formalityRepository.findById(originalFormalityId)).thenReturn(Optional.empty())

        val command = CorrectFormalityCommand(
            originalFormalityId = originalFormalityId,
            lrn = null,
            messageIdentifier = "MSG-002",
            payload = correctedPayload,
            submitterId = submitterId
        )

        assertThrows<FormalityNotFoundException> {
            useCase.execute(command)
        }
    }

    @Test
    fun `should throw ConcurrentCorrectionException when newer version already exists`() {
        val newerFormalityId = UUID.randomUUID()
        val newerFormality = FormalityJpaEntity(
            id = newerFormalityId,
            visit = visitEntity,
            type = FormalityType.NOA,
            version = 2,
            status = FormalityStatus.SUBMITTED,
            submitter = submitterEntity,
            lrn = "REF-002",
            messageIdentifier = "MSG-002",
            channel = SubmissionChannel.WEB
        )

        whenever(formalityRepository.findById(originalFormalityId)).thenReturn(Optional.of(originalFormality))
        // Active version is different from original — concurrent correction detected
        whenever(formalityRepository.findCurrentVersionByVisitIdAndType(visitId, FormalityType.NOA))
            .thenReturn(newerFormality)

        val command = CorrectFormalityCommand(
            originalFormalityId = originalFormalityId,
            lrn = null,
            messageIdentifier = "MSG-003",
            payload = correctedPayload,
            submitterId = submitterId
        )

        assertThrows<ConcurrentCorrectionException> {
            useCase.execute(command)
        }
    }

    @Test
    fun `should publish FormalitySubmittedEvent after successful correction`() {
        whenever(formalityRepository.findById(originalFormalityId)).thenReturn(Optional.of(originalFormality))
        whenever(formalityRepository.findCurrentVersionByVisitIdAndType(visitId, FormalityType.NOA))
            .thenReturn(originalFormality)
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }
        whenever(noaPayloadRepository.save(any())).thenAnswer { it.arguments[0] }

        val command = CorrectFormalityCommand(
            originalFormalityId = originalFormalityId,
            lrn = "REF-002",
            messageIdentifier = "MSG-002",
            payload = correctedPayload,
            submitterId = submitterId
        )

        useCase.execute(command)

        val eventCaptor = argumentCaptor<FormalitySubmittedEvent>()
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture())
        val event = eventCaptor.firstValue
        assertEquals(visitId, event.visitId)
        assertEquals(FormalityType.NOA, event.type)
        assertEquals("NLRTM", event.portLocode)
        assertEquals("MSG-002", event.messageIdentifier)
    }

    @Test
    fun `should increment version number correctly`() {
        val version2Formality = FormalityJpaEntity(
            id = UUID.randomUUID(),
            visit = visitEntity,
            type = FormalityType.NOA,
            version = 2,
            status = FormalityStatus.SUBMITTED,
            submitter = submitterEntity,
            messageIdentifier = "MSG-v2",
            channel = SubmissionChannel.WEB
        )

        whenever(formalityRepository.findById(version2Formality.id)).thenReturn(Optional.of(version2Formality))
        whenever(formalityRepository.findCurrentVersionByVisitIdAndType(visitId, FormalityType.NOA))
            .thenReturn(version2Formality)
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }
        whenever(noaPayloadRepository.save(any())).thenAnswer { it.arguments[0] }

        val command = CorrectFormalityCommand(
            originalFormalityId = version2Formality.id,
            lrn = "REF-003",
            messageIdentifier = "MSG-003",
            payload = correctedPayload,
            submitterId = submitterId
        )

        val result = useCase.execute(command)
        assertEquals(3, result.version)
    }
}
