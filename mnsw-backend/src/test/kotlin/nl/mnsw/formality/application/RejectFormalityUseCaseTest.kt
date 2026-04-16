package nl.mnsw.formality.application

import nl.mnsw.auth.domain.Role
import nl.mnsw.auth.infrastructure.persistence.UserJpaEntity
import nl.mnsw.auth.infrastructure.persistence.UserRepository
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.formality.infrastructure.persistence.FormalityJpaEntity
import nl.mnsw.formality.infrastructure.persistence.FormalityRepository
import nl.mnsw.formality.infrastructure.persistence.FrmResponseJpaEntity
import nl.mnsw.formality.infrastructure.persistence.FrmResponseRepository
import nl.mnsw.formality.infrastructure.persistence.FrmStatus
import nl.mnsw.shared.exception.FormalityNotFoundException
import nl.mnsw.shared.exception.UnauthorizedAccessException
import nl.mnsw.visit.infrastructure.persistence.VisitJpaEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.util.Optional
import java.util.UUID

class RejectFormalityUseCaseTest {

    private val formalityRepository: FormalityRepository = mock()
    private val frmResponseRepository: FrmResponseRepository = mock()
    private val userRepository: UserRepository = mock()
    private val applicationEventPublisher: ApplicationEventPublisher = mock()

    private val useCase = RejectFormalityUseCase(
        formalityRepository = formalityRepository,
        frmResponseRepository = frmResponseRepository,
        userRepository = userRepository,
        applicationEventPublisher = applicationEventPublisher
    )

    private val formalityId = UUID.randomUUID()
    private val reviewerUserId = UUID.randomUUID()

    private val visitEntity = VisitJpaEntity(id = UUID.randomUUID(), imoNumber = "9234567", vesselName = "MV Rotterdam", portLocode = "NLRTM")
    private val submitter = UserJpaEntity(id = UUID.randomUUID(), email = "agent@test.nl", passwordHash = "x", fullName = "Agent")
    private val formalityEntity = FormalityJpaEntity(
        id = formalityId,
        visit = visitEntity,
        type = FormalityType.NOA,
        version = 1,
        status = FormalityStatus.SUBMITTED,
        submitter = submitter,
        messageIdentifier = "MSG-001",
        channel = SubmissionChannel.WEB
    )
    private val reviewerEntity = UserJpaEntity(
        id = reviewerUserId,
        email = "autoriteit@nlrtm.nl",
        passwordHash = "x",
        fullName = "Haven Autoriteit",
        role = Role.HAVENAUTORITEIT,
        portLocode = "NLRTM"
    )

    @Test
    fun `should reject formality and set status to REJECTED`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(userRepository.findById(reviewerUserId)).thenReturn(Optional.of(reviewerEntity))
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }
        whenever(frmResponseRepository.save(any())).thenAnswer { it.arguments[0] as FrmResponseJpaEntity }

        val command = RejectFormalityCommand(
            formalityId = formalityId,
            reasonCode = "INVALID_IMO",
            reasonDescription = "Het opgegeven IMO-nummer is ongeldig",
            reviewerUserId = reviewerUserId
        )

        val result = useCase.execute(command)

        assertEquals(FormalityStatus.REJECTED, result.status)
        assertEquals("INVALID_IMO", result.reasonCode)
        assertEquals("Het opgegeven IMO-nummer is ongeldig", result.reasonDescription)
        assertEquals(FormalityStatus.REJECTED, formalityEntity.status)
    }

    @Test
    fun `should create FRM response with REJECTED status and reason`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(userRepository.findById(reviewerUserId)).thenReturn(Optional.of(reviewerEntity))
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }

        val frmCaptor = argumentCaptor<FrmResponseJpaEntity>()
        whenever(frmResponseRepository.save(frmCaptor.capture())).thenAnswer { it.arguments[0] as FrmResponseJpaEntity }

        val command = RejectFormalityCommand(
            formalityId = formalityId,
            reasonCode = "MISSING_DOCS",
            reasonDescription = "Documenten ontbreken",
            reviewerUserId = reviewerUserId
        )

        useCase.execute(command)

        val savedFrm = frmCaptor.firstValue
        assertEquals(FrmStatus.REJECTED, savedFrm.status)
        assertEquals("MISSING_DOCS", savedFrm.reasonCode)
        assertEquals("Documenten ontbreken", savedFrm.reasonDescription)
    }

    @Test
    fun `should publish FrmResponseCreatedEvent with reason after rejection`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(userRepository.findById(reviewerUserId)).thenReturn(Optional.of(reviewerEntity))
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }
        whenever(frmResponseRepository.save(any())).thenAnswer { it.arguments[0] as FrmResponseJpaEntity }

        val command = RejectFormalityCommand(
            formalityId = formalityId,
            reasonCode = "INVALID_DATA",
            reasonDescription = "Ongeldige gegevens",
            reviewerUserId = reviewerUserId
        )

        useCase.execute(command)

        val eventCaptor = argumentCaptor<FrmResponseCreatedEvent>()
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture())
        val event = eventCaptor.firstValue
        assertEquals(FrmStatus.REJECTED, event.frmStatus)
        assertEquals("INVALID_DATA", event.reasonCode)
        assertEquals("Ongeldige gegevens", event.reasonDescription)
    }

    @Test
    fun `should throw UnauthorizedAccessException when reviewer is from wrong port`() {
        val wrongPortReviewer = UserJpaEntity(
            id = reviewerUserId,
            email = "auth@amst.nl",
            passwordHash = "x",
            fullName = "Amsterdam Autoriteit",
            role = Role.HAVENAUTORITEIT,
            portLocode = "NLAMS"
        )
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(userRepository.findById(reviewerUserId)).thenReturn(Optional.of(wrongPortReviewer))

        assertThrows<UnauthorizedAccessException> {
            useCase.execute(RejectFormalityCommand(formalityId, "CODE", "Reden", reviewerUserId))
        }
    }

    @Test
    fun `should throw FormalityNotFoundException when formality does not exist`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.empty())

        assertThrows<FormalityNotFoundException> {
            useCase.execute(RejectFormalityCommand(formalityId, "CODE", "Reden", reviewerUserId))
        }
    }
}
