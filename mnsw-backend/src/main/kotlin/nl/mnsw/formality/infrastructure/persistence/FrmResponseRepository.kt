package nl.mnsw.formality.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository voor FrmResponseJpaEntity.
 *
 * FRM (Formality Response Message) is een uitgaand antwoord van MNSW aan de indiener.
 * Per formality is er maximaal één actieve FRM response (de meest recente beoordeling).
 * findByFormalityId wordt gebruikt om de status van een ingediende formality te controleren.
 */
@Repository
interface FrmResponseRepository : JpaRepository<FrmResponseJpaEntity, UUID> {

    /**
     * Zoek de FRM response voor een specifieke formality.
     * Retourneert null als er nog geen response is (formality is nog niet beoordeeld).
     */
    fun findByFormalityId(formalityId: UUID): FrmResponseJpaEntity?
}
