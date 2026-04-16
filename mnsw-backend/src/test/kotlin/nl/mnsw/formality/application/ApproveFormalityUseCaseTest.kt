package nl.mnsw.formality.application

import nl.mnsw.auth.domain.Role
import nl.mnsw.auth.infrastructure.persistence.UserJpaEntity
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.formality.infrastructure.persistence.FormalityJpaEntity
import nl.mnsw.formality.infrastructure.persistence.FormalityRepository
import nl.mnsw.formality.infrastructure.persistence.FrmResponseJpaEntity
import nl.mnsw.formality.infrastructure.persistence.FrmResponseRepository
import nl.mnsw.formality.infrastructure.persistence.FrmStatus
import nl.mnsw.auth.infrastructure.persistence.UserRepository
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
import java.util.Optional
import java.util.UUID

class ApproveFormalityUseCaseTest {

    private val formalityRepository: FormalityRepository = mock()
    private val frmResponseRepository: FrmResponseRepository = mock()
    private val userRepository: UserRepository = mock()
    private val applicationEventPublisher: ApplicationEventPublisher = mock()

    private val useCase = ApproveFormalityUseCase(
        formalityRepository = formalityRepository,
        frmResponseRepository = frmResponseRepository,
        userRepository = userRepository,
        applicationEventPublisher = applicationEventPublisher
    )

    private val formalityId = UUID.randomUUID()
    private val reviewerUserId = UUID.randomUUID()
    private val submitterId = UUID.randomUUID()

    private val visitEntity = VisitJpaEntity(
        id = UUID.randomUUID(),
        imoNumber = "9234567",
        vesselName = "MV Rotterdam",
        portLocode = "NLRTM"
    )
    private val submitterEntity = UserJpaEntity(id = submitterId, email = "agent@test.nl", passwordHash = "x", fullName = "Agent")
    private val formalityEntity = FormalityJpaEntity(
        id = formalityId,
        visit = visitEntity,
        type = FormalityType.NOA,
        version = 1,
        status = FormalityStatus.SUBMITTED,
        submitter = submitterEntity,
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
    fun `should approve formality and set status to ACCEPTED`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(userRepository.findById(reviewerUserId)).thenReturn(Optional.of(reviewerEntity))
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }
        whenever(frmResponseRepository.save(any())).thenAnswer { it.arguments[0] as FrmResponseJpaEntity }

        val command = ApproveFormalityCommand(formalityId = formalityId, reviewerUserId = reviewerUserId)
        val result = useCase.execute(command)

        assertEquals(formalityId, result.formalityId)
        assertEquals(FormalityStatus.ACCEPTED, result.status)
        assertNotNull(result.frmSentAt)
        assertEquals(FormalityStatus.ACCEPTED, formalityEntity.status)
    }

    @Test
    fun `should create FRM response with ACCEPTED status`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(userRepository.findById(reviewerUserId)).thenReturn(Optional.of(reviewerEntity))
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }

        val frmCaptor = argumentCaptor<FrmResponseJpaEntity>()
        whenever(frmResponseRepository.save(frmCaptor.capture())).thenAnswer { it.arguments[0] as FrmResponseJpaEntity }

        val command = ApproveFormalityCommand(formalityId = formalityId, reviewerUserId = reviewerUserId)
        useCase.execute(command)

        val savedFrm = frmCaptor.firstValue
        assertEquals(FrmStatus.ACCEPTED, savedFrm.status)
        assertEquals(formalityId, savedFrm.formality.id)
    }

    @Test
    fun `should publish FrmResponseCreatedEvent after approval`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(userRepository.findById(reviewerUserId)).thenReturn(Optional.of(reviewerEntity))
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }
        whenever(frmResponseRepository.save(any())).thenAnswer { it.arguments[0] as FrmResponseJpaEntity }

        val command = ApproveFormalityCommand(formalityId = formalityId, reviewerUserId = reviewerUserId)
        useCase.execute(command)

        val eventCaptor = argumentCaptor<FrmResponseCreatedEvent>()
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture())
        val event = eventCaptor.firstValue
        assertEquals(formalityId, event.formalityId)
        assertEquals(FrmStatus.ACCEPTED, event.frmStatus)
    }

    @Test
    fun `should throw FormalityNotFoundException when formality does not exist`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.empty())

        assertThrows<FormalityNotFoundException> {
            useCase.execute(ApproveFormalityCommand(formalityId, reviewerUserId))
        }
    }

    @Test
    fun `should throw UnauthorizedAccessException when reviewer is from wrong port`() {
        val wrongPortReviewer = UserJpaEntity(
            id = reviewerUserId,
            email = "autoriteit@deham.de",
            passwordHash = "x",
            fullName = "Hamburg Autoriteit",
            role = Role.HAVENAUTORITEIT,
            portLocode = "DEHAM"  // Andere haven
        )

        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(userRepository.findById(reviewerUserId)).thenReturn(Optional.of(wrongPortReviewer))

        assertThrows<UnauthorizedAccessException> {
            useCase.execute(ApproveFormalityCommand(formalityId, reviewerUserId))
        }
    }

    @Test
    fun `should allow ADMIN reviewer without portLocode restriction`() {
        val adminReviewer = UserJpaEntity(
            id = reviewerUserId,
            email = "admin@mnsw.nl",
            passwordHash = "x",
            fullName = "MNSW Admin",
            role = Role.ADMIN,
            portLocode = null  // Geen portLocode voor ADMIN
        )

        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(userRepository.findById(reviewerUserId)).thenReturn(Optional.of(adminReviewer))
        whenever(formalityRepository.save(any())).thenAnswer { it.arguments[0] as FormalityJpaEntity }
        whenever(frmResponseRepository.save(any())).thenAnswer { it.arguments[0] as FrmResponseJpaEntity }

        val result = useCase.execute(ApproveFormalityCommand(formalityId, reviewerUserId))

        assertEquals(FormalityStatus.ACCEPTED, result.status)
    }
}
