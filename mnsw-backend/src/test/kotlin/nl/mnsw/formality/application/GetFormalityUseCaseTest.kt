package nl.mnsw.formality.application

import nl.mnsw.auth.domain.Role
import nl.mnsw.auth.infrastructure.persistence.UserJpaEntity
import nl.mnsw.formality.domain.Formality
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.formality.domain.payload.FormalityPayload
import nl.mnsw.formality.infrastructure.persistence.FormalityJpaEntity
import nl.mnsw.formality.infrastructure.persistence.FormalityRepository
import nl.mnsw.formality.infrastructure.web.FormalityMapper
import nl.mnsw.shared.exception.FormalityNotFoundException
import nl.mnsw.shared.exception.UnauthorizedAccessException
import nl.mnsw.visit.infrastructure.persistence.VisitJpaEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class GetFormalityUseCaseTest {

    private val formalityRepository: FormalityRepository = mock()
    private val formalityMapper: FormalityMapper = mock()

    private val useCase = GetFormalityUseCase(
        formalityRepository = formalityRepository,
        formalityMapper = formalityMapper
    )

    private val submitterId = UUID.randomUUID()
    private val formalityId = UUID.randomUUID()
    private val visitId = UUID.randomUUID()

    private val visitEntity = VisitJpaEntity(id = visitId, imoNumber = "9234567", vesselName = "MV Rotterdam", portLocode = "NLRTM")
    private val submitter = UserJpaEntity(id = submitterId, email = "agent@test.nl", passwordHash = "x", fullName = "Agent")
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
    private val domainFormality = Formality(
        id = formalityId,
        visitId = visitId,
        type = FormalityType.NOA,
        version = 1,
        status = FormalityStatus.SUBMITTED,
        submitterId = submitterId,
        lrn = null,
        messageIdentifier = "MSG-001",
        submittedAt = OffsetDateTime.now(),
        supersededBy = null,
        channel = SubmissionChannel.WEB,
        payload = FormalityPayload.NoaPayload(
            expectedArrival = OffsetDateTime.now().plusDays(2),
            lastPortLocode = null,
            nextPortLocode = null,
            purposeOfCall = null,
            personsOnBoard = null,
            dangerousGoods = false,
            wasteDelivery = false,
            maxStaticDraught = null
        )
    )

    @Test
    fun `should return formality for AGENT who is the submitter`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(formalityRepository.findVersionHistory(visitId, FormalityType.NOA)).thenReturn(listOf(formalityEntity))
        whenever(formalityMapper.toDomain(formalityEntity)).thenReturn(domainFormality)

        val query = GetFormalityQuery(
            formalityId = formalityId,
            requestingUserId = submitterId,
            requestingUserRole = Role.SCHEEPSAGENT,
            requestingUserPortLocode = null
        )

        val result = useCase.execute(query)
        assertEquals(formalityId, result.formality.id)
    }

    @Test
    fun `should throw UnauthorizedAccessException for AGENT who is not the submitter`() {
        val differentUserId = UUID.randomUUID()
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))

        val query = GetFormalityQuery(
            formalityId = formalityId,
            requestingUserId = differentUserId,
            requestingUserRole = Role.SCHEEPSAGENT,
            requestingUserPortLocode = null
        )

        assertThrows<UnauthorizedAccessException> {
            useCase.execute(query)
        }
    }

    @Test
    fun `should return formality for HAVENAUTORITEIT of the same port`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(formalityRepository.findVersionHistory(visitId, FormalityType.NOA)).thenReturn(listOf(formalityEntity))
        whenever(formalityMapper.toDomain(formalityEntity)).thenReturn(domainFormality)

        val query = GetFormalityQuery(
            formalityId = formalityId,
            requestingUserId = UUID.randomUUID(),
            requestingUserRole = Role.HAVENAUTORITEIT,
            requestingUserPortLocode = "NLRTM"
        )

        val result = useCase.execute(query)
        assertEquals(formalityId, result.formality.id)
    }

    @Test
    fun `should throw UnauthorizedAccessException for HAVENAUTORITEIT of different port`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))

        val query = GetFormalityQuery(
            formalityId = formalityId,
            requestingUserId = UUID.randomUUID(),
            requestingUserRole = Role.HAVENAUTORITEIT,
            requestingUserPortLocode = "DEHAM"  // Andere haven
        )

        assertThrows<UnauthorizedAccessException> {
            useCase.execute(query)
        }
    }

    @Test
    fun `should return formality for ADMIN regardless of port`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(formalityRepository.findVersionHistory(visitId, FormalityType.NOA)).thenReturn(listOf(formalityEntity))
        whenever(formalityMapper.toDomain(formalityEntity)).thenReturn(domainFormality)

        val query = GetFormalityQuery(
            formalityId = formalityId,
            requestingUserId = UUID.randomUUID(),
            requestingUserRole = Role.ADMIN,
            requestingUserPortLocode = null
        )

        val result = useCase.execute(query)
        assertEquals(formalityId, result.formality.id)
    }

    @Test
    fun `should throw FormalityNotFoundException when formality does not exist`() {
        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.empty())

        val query = GetFormalityQuery(
            formalityId = formalityId,
            requestingUserId = submitterId,
            requestingUserRole = Role.ADMIN,
            requestingUserPortLocode = null
        )

        assertThrows<FormalityNotFoundException> {
            useCase.execute(query)
        }
    }

    @Test
    fun `should return version history for the formality`() {
        val v1Entity = formalityEntity
        val v2Entity = FormalityJpaEntity(
            id = UUID.randomUUID(),
            visit = visitEntity,
            type = FormalityType.NOA,
            version = 2,
            status = FormalityStatus.SUBMITTED,
            submitter = submitter,
            messageIdentifier = "MSG-002",
            channel = SubmissionChannel.WEB
        )
        val v2Domain = domainFormality.copy(id = v2Entity.id, version = 2, messageIdentifier = "MSG-002")

        whenever(formalityRepository.findById(formalityId)).thenReturn(Optional.of(formalityEntity))
        whenever(formalityRepository.findVersionHistory(visitId, FormalityType.NOA)).thenReturn(listOf(v1Entity, v2Entity))
        whenever(formalityMapper.toDomain(v1Entity)).thenReturn(domainFormality)
        whenever(formalityMapper.toDomain(v2Entity)).thenReturn(v2Domain)

        val query = GetFormalityQuery(
            formalityId = formalityId,
            requestingUserId = submitterId,
            requestingUserRole = Role.SCHEEPSAGENT,
            requestingUserPortLocode = null
        )

        val result = useCase.execute(query)
        assertEquals(2, result.versionHistory.size)
        assertEquals(1, result.versionHistory[0].version)
        assertEquals(2, result.versionHistory[1].version)
    }
}
