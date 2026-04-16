package nl.mnsw.visit.application

import nl.mnsw.visit.infrastructure.persistence.VisitJpaEntity
import nl.mnsw.visit.infrastructure.persistence.VisitRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Use case voor het aanmaken van een nieuw havenbezoek (Visit).
 * Wordt aangemaakt bij de eerste NOA voor een schip+haven+periode combinatie.
 * Toegestaan voor SCHEEPSAGENT, LADINGAGENT, ADMIN.
 */
@Service
@Transactional
class CreateVisitUseCase(
    private val visitRepository: VisitRepository
) {

    fun execute(command: CreateVisitCommand): CreateVisitResult {
        val visitId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        val visitEntity = VisitJpaEntity(
            id = visitId,
            imoNumber = command.imoNumber,
            vesselName = command.vesselName,
            vesselFlag = command.vesselFlag,
            portLocode = command.portLocode,
            eta = command.eta,
            etd = command.etd,
            createdAt = now
        )

        visitRepository.save(visitEntity)

        return CreateVisitResult(visitId = visitId)
    }
}

data class CreateVisitCommand(
    val imoNumber: String,
    val vesselName: String,
    val vesselFlag: String?,
    val portLocode: String,
    val eta: OffsetDateTime?,
    val etd: OffsetDateTime?
)

data class CreateVisitResult(
    val visitId: UUID
)
