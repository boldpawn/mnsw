package nl.mnsw.formality.application

import nl.mnsw.auth.domain.Role
import nl.mnsw.formality.domain.Formality
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.infrastructure.persistence.FormalityRepository
import nl.mnsw.formality.infrastructure.web.FormalityMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Use case voor het ophalen van een gepagineerde lijst formalities.
 * Rolgebaseerde filtering:
 *  - SCHEEPSAGENT/LADINGAGENT: alleen eigen formalities (submitter_id filter)
 *  - HAVENAUTORITEIT: alle formalities voor eigen haven (portLocode filter)
 *  - ADMIN: alles
 *
 * Geen @PreAuthorize annotatie — filtering wordt in de use case zelf afgehandeld op basis van rol.
 */
@Service
@Transactional(readOnly = true)
class ListFormalitiesUseCase(
    private val formalityRepository: FormalityRepository,
    private val formalityMapper: FormalityMapper
) {

    fun execute(query: ListFormalitiesQuery): Page<Formality> {
        return when (query.requestingUserRole) {
            Role.SCHEEPSAGENT, Role.LADINGAGENT -> {
                formalityRepository.findBySubmitterId(query.requestingUserId, query.pageable)
                    .map { formalityMapper.toDomain(it) }
            }
            Role.HAVENAUTORITEIT -> {
                val portLocode = query.requestingUserPortLocode
                    ?: throw IllegalStateException("Havenautoriteit heeft geen portLocode geconfigureerd")

                // Gebruik status-filter voor havenpagina als die is opgegeven, anders eerste aangeboden status
                val statusFilter = query.statusFilter ?: nl.mnsw.formality.domain.FormalityStatus.SUBMITTED

                formalityRepository.findByVisitPortLocodeAndStatus(portLocode, statusFilter, query.pageable)
                    .map { formalityMapper.toDomain(it) }
            }
            Role.ADMIN -> {
                formalityRepository.findAll(query.pageable)
                    .map { formalityMapper.toDomain(it) }
            }
        }
    }
}

data class ListFormalitiesQuery(
    val requestingUserId: UUID,
    val requestingUserRole: Role,
    val requestingUserPortLocode: String?,
    val typeFilter: FormalityType?,
    val statusFilter: FormalityStatus?,
    val visitIdFilter: UUID?,
    val includeSuperseded: Boolean = false,
    val pageable: Pageable
)
