package nl.mnsw.formality.infrastructure.persistence

import nl.mnsw.auth.domain.Role
import nl.mnsw.auth.infrastructure.persistence.UserJpaEntity
import nl.mnsw.auth.infrastructure.persistence.UserRepository
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.visit.infrastructure.persistence.VisitJpaEntity
import nl.mnsw.visit.infrastructure.persistence.VisitRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Integratietests voor FormalityRepository met echte PostgreSQL via Testcontainers.
 *
 * Gebruikt @DataJpaTest voor een gefocuste JPA-slice test — laadt alleen JPA-gerelateerde
 * beans (repositories, entities, Flyway) zonder de volledige Spring-context.
 * Dit voorkomt het laden van Pulsar, Security, en Web-configuratie.
 *
 * @DynamicPropertySource overschrijft de datasource URL naar de Testcontainers PostgreSQL-instance.
 * Flyway migraties draaien automatisch bij startup (alle V1-V10 scripts).
 *
 * Testcontainers vereist Docker draaiend op de dev-machine.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class FormalityRepositoryIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
            .withDatabaseName("mnsw_test")
            .withUsername("mnsw_test")
            .withPassword("mnsw_test")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            // Override Testcontainers JDBC URL prefix die anders in application-test.yml staat
            registry.add("spring.flyway.url", postgres::getJdbcUrl)
            registry.add("spring.flyway.user", postgres::getUsername)
            registry.add("spring.flyway.password", postgres::getPassword)
        }
    }

    @Autowired
    private lateinit var formalityRepository: FormalityRepository

    @Autowired
    private lateinit var visitRepository: VisitRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var noaPayloadRepository: NoaPayloadRepository

    // Test fixtures
    private lateinit var agentA: UserJpaEntity
    private lateinit var agentB: UserJpaEntity
    private lateinit var visitRtm: VisitJpaEntity
    private lateinit var visitAms: VisitJpaEntity

    @BeforeEach
    fun setUp() {
        // Opruimen voor elke test — omgekeerde volgorde vanwege FK-constraints
        noaPayloadRepository.deleteAll()
        formalityRepository.deleteAll()
        visitRepository.deleteAll()
        userRepository.deleteAll()

        // Maak twee agenten aan
        agentA = userRepository.save(
            UserJpaEntity(
                id = UUID.randomUUID(),
                email = "agent.a@rederij.nl",
                passwordHash = "\$2a\$10\$hash",
                fullName = "Agent A",
                role = Role.SCHEEPSAGENT
            )
        )

        agentB = userRepository.save(
            UserJpaEntity(
                id = UUID.randomUUID(),
                email = "agent.b@rederij.nl",
                passwordHash = "\$2a\$10\$hash",
                fullName = "Agent B",
                role = Role.SCHEEPSAGENT
            )
        )

        // Maak twee visits aan in verschillende havens
        visitRtm = visitRepository.save(
            VisitJpaEntity(
                id = UUID.randomUUID(),
                imoNumber = "9074729",
                vesselName = "MV Rotterdam",
                portLocode = "NLRTM"
            )
        )

        visitAms = visitRepository.save(
            VisitJpaEntity(
                id = UUID.randomUUID(),
                imoNumber = "9355058",
                vesselName = "MV Amsterdam",
                portLocode = "NLAMS"
            )
        )
    }

    // ===== Helper: maak een formality aan =====

    private fun saveFormality(
        visit: VisitJpaEntity,
        submitter: UserJpaEntity,
        type: FormalityType = FormalityType.NOA,
        status: FormalityStatus = FormalityStatus.SUBMITTED,
        messageIdentifier: String = "MSG-${UUID.randomUUID()}",
        version: Int = 1
    ): FormalityJpaEntity {
        return formalityRepository.save(
            FormalityJpaEntity(
                id = UUID.randomUUID(),
                visit = visit,
                type = type,
                version = version,
                status = status,
                submitter = submitter,
                lrn = "LRN-${UUID.randomUUID()}",
                messageIdentifier = messageIdentifier,
                submittedAt = OffsetDateTime.now(ZoneOffset.UTC),
                channel = SubmissionChannel.WEB
            )
        )
    }

    // ===== Test: findBySubmitterId =====

    @Test
    fun `findBySubmitterId should return only formalities of the authenticated user`() {
        // Agent A dient 2 formalities in, Agent B dient er 1 in
        saveFormality(visitRtm, agentA)
        saveFormality(visitRtm, agentA, type = FormalityType.NOS)
        saveFormality(visitRtm, agentB)

        val pageable = PageRequest.of(0, 10)
        val resultA = formalityRepository.findBySubmitterId(agentA.id, pageable)
        val resultB = formalityRepository.findBySubmitterId(agentB.id, pageable)

        assertEquals(2, resultA.totalElements, "Agent A moet 2 formalities hebben")
        assertEquals(1, resultB.totalElements, "Agent B moet 1 formality hebben")
        assertTrue(resultA.content.all { it.submitter.id == agentA.id })
        assertTrue(resultB.content.all { it.submitter.id == agentB.id })
    }

    @Test
    fun `findBySubmitterId should return empty page when submitter has no formalities`() {
        val unknownSubmitterId = UUID.randomUUID()
        val result = formalityRepository.findBySubmitterId(unknownSubmitterId, PageRequest.of(0, 10))
        assertEquals(0, result.totalElements)
    }

    // ===== Test: findByVisitPortLocodeAndStatus =====

    @Test
    fun `findByVisitPortLocodeAndStatus should filter correctly on port and status`() {
        // NLRTM: 1 SUBMITTED + 1 ACCEPTED
        saveFormality(visitRtm, agentA, status = FormalityStatus.SUBMITTED)
        saveFormality(visitRtm, agentA, status = FormalityStatus.ACCEPTED)
        // NLAMS: 1 SUBMITTED
        saveFormality(visitAms, agentB, status = FormalityStatus.SUBMITTED)

        val pageable = PageRequest.of(0, 10)
        val rtmSubmitted = formalityRepository.findByVisitPortLocodeAndStatus("NLRTM", FormalityStatus.SUBMITTED, pageable)
        val rtmAccepted = formalityRepository.findByVisitPortLocodeAndStatus("NLRTM", FormalityStatus.ACCEPTED, pageable)
        val amsSubmitted = formalityRepository.findByVisitPortLocodeAndStatus("NLAMS", FormalityStatus.SUBMITTED, pageable)

        assertEquals(1, rtmSubmitted.totalElements, "NLRTM SUBMITTED moet 1 resultaat geven")
        assertEquals(1, rtmAccepted.totalElements, "NLRTM ACCEPTED moet 1 resultaat geven")
        assertEquals(1, amsSubmitted.totalElements, "NLAMS SUBMITTED moet 1 resultaat geven")
        assertTrue(rtmSubmitted.content.all { it.visit.portLocode == "NLRTM" && it.status == FormalityStatus.SUBMITTED })
    }

    @Test
    fun `findByVisitPortLocodeAndStatus should return empty page for unknown port`() {
        saveFormality(visitRtm, agentA)

        val result = formalityRepository.findByVisitPortLocodeAndStatus("USNYC", FormalityStatus.SUBMITTED, PageRequest.of(0, 10))
        assertEquals(0, result.totalElements)
    }

    // ===== Test: findCurrentVersionByVisitIdAndType =====

    @Test
    fun `findCurrentVersionByVisitIdAndType should return only non-SUPERSEDED formality`() {
        // Versie 1: SUPERSEDED
        val v1 = saveFormality(visitRtm, agentA, status = FormalityStatus.SUPERSEDED, version = 1)
        // Versie 2: SUBMITTED (actief)
        val v2 = saveFormality(visitRtm, agentA, status = FormalityStatus.SUBMITTED, version = 2)

        val result = formalityRepository.findCurrentVersionByVisitIdAndType(visitRtm.id, FormalityType.NOA)

        assertNotNull(result, "Er moet een actieve versie gevonden worden")
        assertEquals(v2.id, result!!.id, "De actieve versie moet versie 2 zijn")
        assertEquals(FormalityStatus.SUBMITTED, result.status)
    }

    @Test
    fun `findCurrentVersionByVisitIdAndType should return null when all versions are superseded`() {
        saveFormality(visitRtm, agentA, status = FormalityStatus.SUPERSEDED)

        val result = formalityRepository.findCurrentVersionByVisitIdAndType(visitRtm.id, FormalityType.NOA)
        assertNull(result, "Er mag geen actieve versie zijn als alles SUPERSEDED is")
    }

    @Test
    fun `findCurrentVersionByVisitIdAndType should return null when no formality exists for type`() {
        saveFormality(visitRtm, agentA, type = FormalityType.NOS)

        val result = formalityRepository.findCurrentVersionByVisitIdAndType(visitRtm.id, FormalityType.NOA)
        assertNull(result, "NOA bestaat niet voor deze visit")
    }

    @Test
    fun `findCurrentVersionByVisitIdAndType should return accepted version as current`() {
        // ACCEPTED formalities zijn ook niet-SUPERSEDED en zijn dus de huidige versie
        val accepted = saveFormality(visitRtm, agentA, status = FormalityStatus.ACCEPTED)

        val result = formalityRepository.findCurrentVersionByVisitIdAndType(visitRtm.id, FormalityType.NOA)
        assertNotNull(result)
        assertEquals(accepted.id, result!!.id)
    }

    // ===== Test: existsByMessageIdentifier =====

    @Test
    fun `existsByMessageIdentifier should detect duplicate message identifiers`() {
        val messageId = "MSG-UNIQUE-2026-001"
        saveFormality(visitRtm, agentA, messageIdentifier = messageId)

        assertTrue(
            formalityRepository.existsByMessageIdentifier(messageId),
            "Bestaande Message Identifier moet worden gedetecteerd"
        )
    }

    @Test
    fun `existsByMessageIdentifier should return false for unknown message identifier`() {
        assertFalse(
            formalityRepository.existsByMessageIdentifier("MSG-DOES-NOT-EXIST"),
            "Onbekende Message Identifier mag niet bestaan"
        )
    }

    @Test
    fun `existsByMessageIdentifier should return false after all formalities are deleted`() {
        val messageId = "MSG-DELETE-TEST"
        val formality = saveFormality(visitRtm, agentA, messageIdentifier = messageId)

        assertTrue(formalityRepository.existsByMessageIdentifier(messageId))
        formalityRepository.deleteById(formality.id)
        assertFalse(formalityRepository.existsByMessageIdentifier(messageId))
    }

    // ===== Test: findByIdAndSubmitterId =====

    @Test
    fun `findByIdAndSubmitterId should return formality when id and submitterId match`() {
        val formality = saveFormality(visitRtm, agentA)

        val result = formalityRepository.findByIdAndSubmitterId(formality.id, agentA.id)
        assertNotNull(result)
        assertEquals(formality.id, result!!.id)
    }

    @Test
    fun `findByIdAndSubmitterId should return null when submitterId does not match`() {
        val formality = saveFormality(visitRtm, agentA)

        // agentB probeert de formality van agentA te vinden — mag niet
        val result = formalityRepository.findByIdAndSubmitterId(formality.id, agentB.id)
        assertNull(result, "agentB mag de formality van agentA niet zien")
    }

    // ===== Test: findByVisitId =====

    @Test
    fun `findByVisitId should return all formalities for a visit`() {
        saveFormality(visitRtm, agentA, type = FormalityType.NOA)
        saveFormality(visitRtm, agentA, type = FormalityType.VID)
        saveFormality(visitRtm, agentA, type = FormalityType.SID)
        saveFormality(visitAms, agentB, type = FormalityType.NOA) // Andere visit

        val result = formalityRepository.findByVisitId(visitRtm.id)
        assertEquals(3, result.size, "Visit RTM moet 3 formalities hebben")
        assertTrue(result.all { it.visit.id == visitRtm.id })
    }
}
