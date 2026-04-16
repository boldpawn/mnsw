package nl.mnsw.formality.infrastructure.persistence

import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository voor FormalityJpaEntity.
 *
 * Queries conform de hexagonale architectuurlaag — GEEN domeinlogica hier.
 * Autorisatiefiltering op submitter_id is essentieel: agenten mogen alleen eigen formalities zien.
 *
 * findCurrentVersionByVisitIdAndType filtert expliciet op 'SUPERSEDED' om versiehistorie te
 * verbergen en alleen de actieve versie terug te geven.
 */
@Repository
interface FormalityRepository : JpaRepository<FormalityJpaEntity, UUID> {

    /**
     * Zoek alle formalities van een specifieke indiener, gepagineerd.
     * Gebruikt voor de AGENT rol: eigen ingediende formalities.
     */
    fun findBySubmitterId(submitterId: UUID, pageable: Pageable): Page<FormalityJpaEntity>

    /**
     * Zoek formalities op haven-locode en status, gepagineerd.
     * Gebruikt voor de HAVENAUTORITEIT rol: formalities voor hun haven met bepaalde status.
     * JOIN via de visit-relatie om op portLocode te filteren.
     */
    @Query(
        "SELECT f FROM FormalityJpaEntity f JOIN f.visit v " +
            "WHERE v.portLocode = :portLocode AND f.status = :status"
    )
    fun findByVisitPortLocodeAndStatus(
        portLocode: String,
        status: FormalityStatus,
        pageable: Pageable
    ): Page<FormalityJpaEntity>

    /**
     * Zoek alle formalities gekoppeld aan een visit (alle versies, alle types).
     */
    fun findByVisitId(visitId: UUID): List<FormalityJpaEntity>

    /**
     * Zoek een formality op id en submitter_id — eigenaarcontrole in één query.
     * Retourneert null als de formality niet bestaat of aan een andere indiener toebehoort.
     */
    fun findByIdAndSubmitterId(id: UUID, submitterId: UUID): FormalityJpaEntity?

    /**
     * Zoek de actieve (niet-SUPERSEDED) versie van een formality voor een visit en type.
     * Gebruikt bij de correctiewerkstroom om duplicaat-actieve versies te detecteren.
     *
     * Vergelijking met string 'SUPERSEDED' werkt met PostgreSQL ENUM via Hibernate STRING mapping.
     */
    @Query(
        "SELECT f FROM FormalityJpaEntity f " +
            "WHERE f.visit.id = :visitId AND f.type = :type AND f.status <> 'SUPERSEDED'"
    )
    fun findCurrentVersionByVisitIdAndType(
        visitId: UUID,
        type: FormalityType
    ): FormalityJpaEntity?

    /**
     * Controleer of een Message Identifier al bestaat — duplicaatdetectie conform MAI-header vereiste.
     * Een Message Identifier moet uniek zijn per indiener (MAI spec).
     */
    fun existsByMessageIdentifier(messageIdentifier: String): Boolean

    /**
     * Zoek de volledige versiehistorie van een formality type binnen een visit.
     * Gesorteerd op versienummer oplopend — geeft alle versies terug, inclusief SUPERSEDED.
     */
    @Query(
        "SELECT f FROM FormalityJpaEntity f " +
            "WHERE f.visit.id = :visitId AND f.type = :type " +
            "ORDER BY f.version ASC"
    )
    fun findVersionHistory(visitId: UUID, type: FormalityType): List<FormalityJpaEntity>
}
