package nl.mnsw.formality

import nl.mnsw.auth.domain.Role
import nl.mnsw.auth.infrastructure.persistence.UserJpaEntity
import nl.mnsw.formality.domain.Formality
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.formality.domain.payload.FormalityPayload
import nl.mnsw.formality.domain.payload.PortEntry
import nl.mnsw.formality.infrastructure.persistence.FormalityJpaEntity
import nl.mnsw.formality.infrastructure.persistence.NodPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.NoaPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.NosPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.SidPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.VidPayloadJpaEntity
import nl.mnsw.formality.infrastructure.web.FormalityMapper
import nl.mnsw.shared.exception.ValidationException
import nl.mnsw.visit.infrastructure.persistence.VisitJpaEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Unit tests voor FormalityMapper.
 * Test JPA entiteit -> domain en domain -> JPA entiteit mapping voor alle 5 formality types.
 */
class FormalityMapperTest {

    private val mapper = FormalityMapper()

    // ===== Test fixtures =====

    private val testVisitId = UUID.randomUUID()
    private val testFormalityId = UUID.randomUUID()
    private val testSubmitterId = UUID.randomUUID()
    private val testSubmittedAt = OffsetDateTime.of(2026, 4, 15, 10, 0, 0, 0, ZoneOffset.UTC)

    private fun buildVisitEntity(id: UUID = testVisitId) = VisitJpaEntity(
        id = id,
        imoNumber = "9234567",
        vesselName = "MV Rotterdam",
        vesselFlag = "NLD",
        portLocode = "NLRTM",
        eta = OffsetDateTime.of(2026, 4, 20, 8, 0, 0, 0, ZoneOffset.UTC),
        etd = OffsetDateTime.of(2026, 4, 22, 16, 0, 0, 0, ZoneOffset.UTC)
    )

    private fun buildUserEntity(id: UUID = testSubmitterId) = UserJpaEntity(
        id = id,
        email = "agent@rederij.nl",
        passwordHash = "\$2a\$10\$hash",
        fullName = "Jan Jansen",
        role = Role.SCHEEPSAGENT
    )

    private fun buildBaseFormalityEntity(type: FormalityType, visit: VisitJpaEntity, submitter: UserJpaEntity): FormalityJpaEntity =
        FormalityJpaEntity(
            id = testFormalityId,
            visit = visit,
            type = type,
            version = 1,
            status = FormalityStatus.SUBMITTED,
            submitter = submitter,
            lrn = "AGENT-REF-2026-001",
            messageIdentifier = "MSG-20260415-001",
            submittedAt = testSubmittedAt,
            supersededBy = null,
            channel = SubmissionChannel.WEB
        )

    // ===== NOA toDomain tests =====

    @Test
    fun `toDomain should map NOA entity to domain formality`() {
        val visit = buildVisitEntity()
        val submitter = buildUserEntity()
        val entity = buildBaseFormalityEntity(FormalityType.NOA, visit, submitter)
        val expectedArrival = OffsetDateTime.of(2026, 4, 20, 8, 0, 0, 0, ZoneOffset.UTC)

        entity.noaPayload = NoaPayloadJpaEntity(
            id = testFormalityId,
            formality = entity,
            expectedArrival = expectedArrival,
            lastPortLocode = "GBFXT",
            nextPortLocode = "NLRTM",
            purposeOfCall = "Lossing containers",
            personsOnBoard = 22,
            dangerousGoods = false,
            wasteDelivery = true,
            maxStaticDraught = BigDecimal("11.50")
        )

        val domain = mapper.toDomain(entity)

        assertEquals(testFormalityId, domain.id)
        assertEquals(testVisitId, domain.visitId)
        assertEquals(FormalityType.NOA, domain.type)
        assertEquals(1, domain.version)
        assertEquals(FormalityStatus.SUBMITTED, domain.status)
        assertEquals(testSubmitterId, domain.submitterId)
        assertEquals("AGENT-REF-2026-001", domain.lrn)
        assertEquals("MSG-20260415-001", domain.messageIdentifier)
        assertEquals(testSubmittedAt, domain.submittedAt)
        assertNull(domain.supersededBy)
        assertEquals(SubmissionChannel.WEB, domain.channel)

        val payload = domain.payload as FormalityPayload.NoaPayload
        assertEquals(expectedArrival, payload.expectedArrival)
        assertEquals("GBFXT", payload.lastPortLocode)
        assertEquals("NLRTM", payload.nextPortLocode)
        assertEquals("Lossing containers", payload.purposeOfCall)
        assertEquals(22, payload.personsOnBoard)
        assertEquals(false, payload.dangerousGoods)
        assertEquals(true, payload.wasteDelivery)
        assertEquals(BigDecimal("11.50"), payload.maxStaticDraught)
    }

    @Test
    fun `toDomain should throw ValidationException when NOA payload is missing`() {
        val visit = buildVisitEntity()
        val submitter = buildUserEntity()
        val entity = buildBaseFormalityEntity(FormalityType.NOA, visit, submitter)
        // Geen noaPayload gezet

        val exception = assertThrows<ValidationException> { mapper.toDomain(entity) }
        assertEquals("MISSING_NOA_PAYLOAD", exception.errors.first().code)
    }

    // ===== NOS toDomain tests =====

    @Test
    fun `toDomain should map NOS entity to domain formality`() {
        val visit = buildVisitEntity()
        val submitter = buildUserEntity()
        val entity = buildBaseFormalityEntity(FormalityType.NOS, visit, submitter)
        val actualSailing = OffsetDateTime.of(2026, 4, 22, 14, 30, 0, 0, ZoneOffset.UTC)

        entity.nosPayload = NosPayloadJpaEntity(
            id = testFormalityId,
            formality = entity,
            actualSailing = actualSailing,
            nextPortLocode = "DEHAM",
            destinationCountry = "DEU"
        )

        val domain = mapper.toDomain(entity)
        val payload = domain.payload as FormalityPayload.NosPayload

        assertEquals(actualSailing, payload.actualSailing)
        assertEquals("DEHAM", payload.nextPortLocode)
        assertEquals("DEU", payload.destinationCountry)
    }

    @Test
    fun `toDomain should throw ValidationException when NOS payload is missing`() {
        val visit = buildVisitEntity()
        val submitter = buildUserEntity()
        val entity = buildBaseFormalityEntity(FormalityType.NOS, visit, submitter)

        val exception = assertThrows<ValidationException> { mapper.toDomain(entity) }
        assertEquals("MISSING_NOS_PAYLOAD", exception.errors.first().code)
    }

    // ===== NOD toDomain tests =====

    @Test
    fun `toDomain should map NOD entity to domain formality`() {
        val visit = buildVisitEntity()
        val submitter = buildUserEntity()
        val entity = buildBaseFormalityEntity(FormalityType.NOD, visit, submitter)
        val expectedDeparture = OffsetDateTime.of(2026, 4, 22, 16, 0, 0, 0, ZoneOffset.UTC)
        val lastCargoOps = OffsetDateTime.of(2026, 4, 22, 12, 0, 0, 0, ZoneOffset.UTC)

        entity.nodPayload = NodPayloadJpaEntity(
            id = testFormalityId,
            formality = entity,
            expectedDeparture = expectedDeparture,
            nextPortLocode = "DEHAM",
            destinationCountry = "DEU",
            lastCargoOperations = lastCargoOps
        )

        val domain = mapper.toDomain(entity)
        val payload = domain.payload as FormalityPayload.NodPayload

        assertEquals(expectedDeparture, payload.expectedDeparture)
        assertEquals("DEHAM", payload.nextPortLocode)
        assertEquals("DEU", payload.destinationCountry)
        assertEquals(lastCargoOps, payload.lastCargoOperations)
    }

    @Test
    fun `toDomain should map NOD entity with null optional fields`() {
        val visit = buildVisitEntity()
        val submitter = buildUserEntity()
        val entity = buildBaseFormalityEntity(FormalityType.NOD, visit, submitter)
        val expectedDeparture = OffsetDateTime.of(2026, 4, 22, 16, 0, 0, 0, ZoneOffset.UTC)

        entity.nodPayload = NodPayloadJpaEntity(
            id = testFormalityId,
            formality = entity,
            expectedDeparture = expectedDeparture,
            nextPortLocode = null,
            destinationCountry = null,
            lastCargoOperations = null
        )

        val domain = mapper.toDomain(entity)
        val payload = domain.payload as FormalityPayload.NodPayload

        assertNull(payload.nextPortLocode)
        assertNull(payload.destinationCountry)
        assertNull(payload.lastCargoOperations)
    }

    // ===== VID toDomain tests =====

    @Test
    fun `toDomain should map VID entity to domain formality`() {
        val visit = buildVisitEntity()
        val submitter = buildUserEntity()
        val entity = buildBaseFormalityEntity(FormalityType.VID, visit, submitter)

        entity.vidPayload = VidPayloadJpaEntity(
            id = testFormalityId,
            formality = entity,
            certificateNationality = "NLD",
            grossTonnage = BigDecimal("45000.00"),
            netTonnage = BigDecimal("22000.00"),
            deadweight = BigDecimal("75000.00"),
            lengthOverall = BigDecimal("294.50"),
            shipType = "BULK CARRIER",
            callSign = "PBZK",
            mmsi = "244123456"
        )

        val domain = mapper.toDomain(entity)
        val payload = domain.payload as FormalityPayload.VidPayload

        assertEquals("NLD", payload.certificateNationality)
        assertEquals(BigDecimal("45000.00"), payload.grossTonnage)
        assertEquals(BigDecimal("22000.00"), payload.netTonnage)
        assertEquals(BigDecimal("75000.00"), payload.deadweight)
        assertEquals(BigDecimal("294.50"), payload.lengthOverall)
        assertEquals("BULK CARRIER", payload.shipType)
        assertEquals("PBZK", payload.callSign)
        assertEquals("244123456", payload.mmsi)
    }

    // ===== SID toDomain tests =====

    @Test
    fun `toDomain should map SID entity to domain formality`() {
        val visit = buildVisitEntity()
        val submitter = buildUserEntity()
        val entity = buildBaseFormalityEntity(FormalityType.SID, visit, submitter)
        val portEntries = listOf(
            PortEntry("GBFXT", LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 12), 1),
            PortEntry("FRBOY", LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 9), 1)
        )

        entity.sidPayload = SidPayloadJpaEntity(
            id = testFormalityId,
            formality = entity,
            ispsLevel = 1,
            last10Ports = portEntries,
            securityDeclaration = "PARTIAL",
            shipToShipActivities = false,
            designatedAuthority = null,
            ssasActivated = false
        )

        val domain = mapper.toDomain(entity)
        val payload = domain.payload as FormalityPayload.SidPayload

        assertEquals(1, payload.ispsLevel)
        assertEquals(2, payload.last10Ports.size)
        assertEquals("GBFXT", payload.last10Ports[0].locode)
        assertEquals(LocalDate.of(2026, 4, 10), payload.last10Ports[0].arrivalDate)
        assertEquals("PARTIAL", payload.securityDeclaration)
        assertEquals(false, payload.shipToShipActivities)
        assertNull(payload.designatedAuthority)
    }

    @Test
    fun `toDomain should map SID entity with ispsLevel 2 and designatedAuthority`() {
        val visit = buildVisitEntity()
        val submitter = buildUserEntity()
        val entity = buildBaseFormalityEntity(FormalityType.SID, visit, submitter)

        entity.sidPayload = SidPayloadJpaEntity(
            id = testFormalityId,
            formality = entity,
            ispsLevel = 2,
            last10Ports = emptyList(),
            securityDeclaration = "FULL",
            shipToShipActivities = true,
            designatedAuthority = "Port Security Authority Rotterdam",
            ssasActivated = false
        )

        val domain = mapper.toDomain(entity)
        val payload = domain.payload as FormalityPayload.SidPayload

        assertEquals(2, payload.ispsLevel)
        assertEquals("Port Security Authority Rotterdam", payload.designatedAuthority)
        assertEquals(true, payload.shipToShipActivities)
    }

    // ===== toEntity tests =====

    @Test
    fun `toEntity should map NOA domain formality to entity`() {
        val visitEntity = buildVisitEntity()
        val userEntity = buildUserEntity()
        val expectedArrival = OffsetDateTime.of(2026, 4, 20, 8, 0, 0, 0, ZoneOffset.UTC)

        val domain = Formality(
            id = testFormalityId,
            visitId = testVisitId,
            type = FormalityType.NOA,
            version = 1,
            status = FormalityStatus.SUBMITTED,
            submitterId = testSubmitterId,
            lrn = "AGENT-REF-2026-001",
            messageIdentifier = "MSG-20260415-001",
            submittedAt = testSubmittedAt,
            supersededBy = null,
            channel = SubmissionChannel.WEB,
            payload = FormalityPayload.NoaPayload(
                expectedArrival = expectedArrival,
                lastPortLocode = "GBFXT",
                nextPortLocode = "NLRTM",
                purposeOfCall = "Lossing containers",
                personsOnBoard = 22,
                dangerousGoods = false,
                wasteDelivery = true,
                maxStaticDraught = BigDecimal("11.50")
            )
        )

        val entity = mapper.toEntity(domain, visitEntity, userEntity)

        assertEquals(testFormalityId, entity.id)
        assertEquals(testVisitId, entity.visit.id)
        assertEquals(FormalityType.NOA, entity.type)
        assertEquals(1, entity.version)
        assertEquals(FormalityStatus.SUBMITTED, entity.status)
        assertEquals(testSubmitterId, entity.submitter.id)
        assertEquals(SubmissionChannel.WEB, entity.channel)

        val noaPayload = entity.noaPayload
        assertNotNull(noaPayload)
        assertEquals(expectedArrival, noaPayload!!.expectedArrival)
        assertEquals("GBFXT", noaPayload.lastPortLocode)
        assertEquals(22, noaPayload.personsOnBoard)
        assertEquals(BigDecimal("11.50"), noaPayload.maxStaticDraught)

        // Andere payloads zijn null
        assertNull(entity.nosPayload)
        assertNull(entity.nodPayload)
        assertNull(entity.vidPayload)
        assertNull(entity.sidPayload)
    }

    @Test
    fun `toEntity should map SID domain formality to entity with port entries`() {
        val visitEntity = buildVisitEntity()
        val userEntity = buildUserEntity()
        val portEntries = listOf(
            PortEntry("GBFXT", LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 12), 1)
        )

        val domain = Formality(
            id = testFormalityId,
            visitId = testVisitId,
            type = FormalityType.SID,
            version = 1,
            status = FormalityStatus.SUBMITTED,
            submitterId = testSubmitterId,
            lrn = null,
            messageIdentifier = "MSG-SID-001",
            submittedAt = testSubmittedAt,
            supersededBy = null,
            channel = SubmissionChannel.RIM,
            payload = FormalityPayload.SidPayload(
                ispsLevel = 2,
                last10Ports = portEntries,
                securityDeclaration = "FULL",
                shipToShipActivities = false,
                designatedAuthority = "Port Security Authority Rotterdam",
                ssasActivated = false
            )
        )

        val entity = mapper.toEntity(domain, visitEntity, userEntity)

        assertEquals(SubmissionChannel.RIM, entity.channel)
        val sidPayload = entity.sidPayload
        assertNotNull(sidPayload)
        assertEquals(2, sidPayload!!.ispsLevel)
        assertEquals(1, sidPayload.last10Ports.size)
        assertEquals("GBFXT", sidPayload.last10Ports[0].locode)
        assertEquals("Port Security Authority Rotterdam", sidPayload.designatedAuthority)

        assertNull(entity.noaPayload)
        assertNull(entity.nosPayload)
        assertNull(entity.nodPayload)
        assertNull(entity.vidPayload)
    }

    @Test
    fun `toEntity should preserve supersededBy UUID`() {
        val visitEntity = buildVisitEntity()
        val userEntity = buildUserEntity()
        val supersededById = UUID.randomUUID()

        val domain = Formality(
            id = testFormalityId,
            visitId = testVisitId,
            type = FormalityType.NOS,
            version = 2,
            status = FormalityStatus.SUPERSEDED,
            submitterId = testSubmitterId,
            lrn = null,
            messageIdentifier = "MSG-NOS-002",
            submittedAt = testSubmittedAt,
            supersededBy = supersededById,
            channel = SubmissionChannel.WEB,
            payload = FormalityPayload.NosPayload(
                actualSailing = OffsetDateTime.now(ZoneOffset.UTC),
                nextPortLocode = null,
                destinationCountry = null
            )
        )

        val entity = mapper.toEntity(domain, visitEntity, userEntity)

        assertEquals(supersededById, entity.supersededBy)
        assertEquals(FormalityStatus.SUPERSEDED, entity.status)
        assertEquals(2, entity.version)
    }

    // ===== Round-trip test =====

    @Test
    fun `toDomain and toEntity should be inverse operations for VID`() {
        val visitEntity = buildVisitEntity()
        val userEntity = buildUserEntity()

        val originalDomain = Formality(
            id = testFormalityId,
            visitId = testVisitId,
            type = FormalityType.VID,
            version = 1,
            status = FormalityStatus.ACCEPTED,
            submitterId = testSubmitterId,
            lrn = "LRN-VID-001",
            messageIdentifier = "MSG-VID-001",
            submittedAt = testSubmittedAt,
            supersededBy = null,
            channel = SubmissionChannel.WEB,
            payload = FormalityPayload.VidPayload(
                certificateNationality = "NLD",
                grossTonnage = BigDecimal("45000.00"),
                netTonnage = BigDecimal("22000.00"),
                deadweight = BigDecimal("75000.00"),
                lengthOverall = BigDecimal("294.50"),
                shipType = "BULK CARRIER",
                callSign = "PBZK",
                mmsi = "244123456"
            )
        )

        val entity = mapper.toEntity(originalDomain, visitEntity, userEntity)
        val roundTrippedDomain = mapper.toDomain(entity)

        assertEquals(originalDomain.id, roundTrippedDomain.id)
        assertEquals(originalDomain.type, roundTrippedDomain.type)
        assertEquals(originalDomain.status, roundTrippedDomain.status)
        assertEquals(originalDomain.payload, roundTrippedDomain.payload)
    }
}
