package nl.mnsw.formality.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository voor VidPayloadJpaEntity.
 * PK = formality_id (gedeeld met de formality tabel via @MapsId).
 * Standaard CRUD-operaties zijn voldoende — specifieke queries lopen via FormalityRepository.
 */
@Repository
interface VidPayloadRepository : JpaRepository<VidPayloadJpaEntity, UUID>
