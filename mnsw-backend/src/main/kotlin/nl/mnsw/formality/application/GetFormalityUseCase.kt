package nl.mnsw.formality.application

import nl.mnsw.auth.domain.Role
import nl.mnsw.formality.domain.Formality
import nl.mnsw.formality.infrastructure.persistence.FormalityRepository
import nl.mnsw.formality.infrastructure.web.FormalityMapper
import nl.mnsw.shared.exception.FormalityNotFoundException
import nl.mnsw.shared.exception.UnauthorizedAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Use case voor het ophalen van een formality met versiehistorie.
 * Leesrechten op basis van rol:
 *  - SCHEEPSAGENT/LADINGAGENT: alleen eigen formalities (submitterId == userId)
 *  - HAVENAUTORITEIT: formalities van eigen haven (visit.portLocode == user.portLocode)
 *  - ADMIN: altijd toegang
 */
@Service
@Transactional(readOnly = true)
class GetFormalityUseCase(
    private val formalityRepository: FormalityRepository,
    private val formalityMapper: FormalityMapper
) {

    fun execute(query: GetFormalityQuery): GetFormalityResult {
        // 1. Haal formality op
        val entity = formalityRepository.findById(query.formalityId).orElseThrow {
            FormalityNotFoundException(query.formalityId)
        }

        // 2. Controleer leesrechten op basis van rol
        when (query.requestingUserRole) {
            Role.SCHEEPSAGENT, Role.LADINGAGENT -> {
                if (entity.submitter.id != query.requestingUserId) {
                    throw UnauthorizedAccessException(
                        "Gebruiker ${query.requestingUserId} heeft geen toegang tot formality ${query.formalityId}"
                    )
                }
            }
            Role.HAVENAUTORITEIT -> {
                val userPortLocode = query.requestingUserPortLocode
                    ?: throw UnauthorizedAccessException("Havenautoriteit heeft geen portLocode geconfigureerd")
                if (entity.visit.portLocode != userPortLocode) {
                    throw UnauthorizedAccessException(
                        "Havenautoriteit (haven $userPortLocode) heeft geen toegang tot formalities " +
                            "van haven ${entity.visit.portLocode}"
                    )
                }
            }
            Role.ADMIN -> { /* altijd toegang */ }
        }

        // 3. Haal versiehistorie op
        val versionHistory = formalityRepository.findVersionHistory(
            visitId = entity.visit.id,
            type = entity.type
        )

        val domainFormality = formalityMapper.toDomain(entity)
        val historyDomain = versionHistory.map { formalityMapper.toDomain(it) }

        return GetFormalityResult(
            formality = domainFormality,
            versionHistory = historyDomain
        )
    }
}

data class GetFormalityQuery(
    val formalityId: UUID,
    val requestingUserId: UUID,
    val requestingUserRole: Role,
    val requestingUserPortLocode: String?
)

data class GetFormalityResult(
    val formality: Formality,
    val versionHistory: List<Formality>
)
