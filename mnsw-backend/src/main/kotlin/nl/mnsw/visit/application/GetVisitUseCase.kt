package nl.mnsw.visit.application

import nl.mnsw.formality.infrastructure.persistence.FormalityJpaEntity
import nl.mnsw.formality.infrastructure.persistence.FormalityRepository
import nl.mnsw.shared.exception.VisitNotFoundException
import nl.mnsw.visit.domain.Visit
import nl.mnsw.visit.infrastructure.persistence.VisitRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Use case voor het ophalen van een visit met gekoppelde formalities (huidige versies).
 * Geeft de Visit op en de bijbehorende formalities (niet-SUPERSEDED versies).
 */
@Service
@Transactional(readOnly = true)
class GetVisitUseCase(
    private val visitRepository: VisitRepository,
    private val formalityRepository: FormalityRepository
) {

    fun execute(query: GetVisitQuery): GetVisitResult {
        val visitEntity = visitRepository.findById(query.visitId).orElseThrow {
            VisitNotFoundException(query.visitId)
        }

        val visit = Visit(
            id = visitEntity.id,
            imoNumber = visitEntity.imoNumber,
            vesselName = visitEntity.vesselName,
            vesselFlag = visitEntity.vesselFlag,
            portLocode = visitEntity.portLocode,
            eta = visitEntity.eta,
            etd = visitEntity.etd,
            createdAt = visitEntity.createdAt
        )

        // Haal alle formalities op voor deze visit — alleen actieve versies (niet SUPERSEDED)
        val formalities = formalityRepository.findByVisitId(query.visitId)
            .filter { it.status != nl.mnsw.formality.domain.FormalityStatus.SUPERSEDED }

        return GetVisitResult(
            visit = visit,
            formalities = formalities
        )
    }
}

data class GetVisitQuery(
    val visitId: UUID
)

data class GetVisitResult(
    val visit: Visit,
    val formalities: List<FormalityJpaEntity>
)
