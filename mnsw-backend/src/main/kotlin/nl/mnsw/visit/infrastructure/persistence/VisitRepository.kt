package nl.mnsw.visit.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository voor VisitJpaEntity.
 *
 * Queries conform hexagonale architectuurlaag.
 * Een schip kan meerdere bezoeken aan dezelfde haven hebben (verschillende periodes),
 * daarom retourneert findByImoNumber een lijst.
 */
@Repository
interface VisitRepository : JpaRepository<VisitJpaEntity, UUID> {

    /**
     * Zoek alle havenbezoeken voor een specifieke haven, gepagineerd.
     * Gebruikt door de HAVENAUTORITEIT rol om bezoeken voor hun haven te bekijken.
     */
    fun findByPortLocode(portLocode: String, pageable: Pageable): Page<VisitJpaEntity>

    /**
     * Zoek alle havenbezoeken van een specifiek schip op IMO-nummer.
     * Een schip kan meerdere bezoeken hebben — retourneert de volledige lijst.
     */
    fun findByImoNumber(imoNumber: String): List<VisitJpaEntity>
}
