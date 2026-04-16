package nl.mnsw.formality.application

import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.infrastructure.persistence.FormalityRepository
import nl.mnsw.formality.infrastructure.persistence.FrmResponseJpaEntity
import nl.mnsw.formality.infrastructure.persistence.FrmResponseRepository
import nl.mnsw.formality.infrastructure.persistence.FrmStatus
import nl.mnsw.auth.infrastructure.persistence.UserRepository
import nl.mnsw.shared.exception.FormalityNotFoundException
import nl.mnsw.shared.exception.UnauthorizedAccessException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Use case voor het afwijzen van een formality door de havenautoriteit.
 * Toegestaan voor HAVENAUTORITEIT (eigen haven) en ADMIN.
 *
 * Autorisatie: formality.visit.portLocode == reviewer.portLocode (of ADMIN).
 * Status: SUBMITTED of UNDER_REVIEW -> REJECTED.
 * FRM response bevat reasonCode en reasonDescription conform EMSWe MIG.
 */
@Service
@Transactional
class RejectFormalityUseCase(
    private val formalityRepository: FormalityRepository,
    private val frmResponseRepository: FrmResponseRepository,
    private val userRepository: UserRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun execute(command: RejectFormalityCommand): RejectFormalityResult {
        // 1. Haal formality op
        val formality = formalityRepository.findById(command.formalityId).orElseThrow {
            FormalityNotFoundException(command.formalityId)
        }

        // 2. Controleer haven-autorisatie
        val reviewer = userRepository.findById(command.reviewerUserId).orElseThrow {
            IllegalStateException("Reviewer not found: ${command.reviewerUserId}")
        }

        if (reviewer.portLocode != null && reviewer.portLocode != formality.visit.portLocode) {
            throw UnauthorizedAccessException(
                "Havenautoriteit ${command.reviewerUserId} (haven ${reviewer.portLocode}) " +
                    "heeft geen toegang tot formalities van haven ${formality.visit.portLocode}"
            )
        }

        // 3. Zet status op REJECTED
        formality.status = FormalityStatus.REJECTED
        formalityRepository.save(formality)

        // 4. Maak FRM response aan met reden
        val now = OffsetDateTime.now()
        val frmResponse = FrmResponseJpaEntity(
            id = UUID.randomUUID(),
            formality = formality,
            status = FrmStatus.REJECTED,
            reasonCode = command.reasonCode,
            reasonDescription = command.reasonDescription,
            sentAt = now,
            channel = formality.channel
        )
        frmResponseRepository.save(frmResponse)

        // 5. Publiceer event voor RIM (after-commit via @TransactionalEventListener)
        applicationEventPublisher.publishEvent(
            FrmResponseCreatedEvent(
                formalityId = command.formalityId,
                frmStatus = FrmStatus.REJECTED,
                reasonCode = command.reasonCode,
                reasonDescription = command.reasonDescription,
                channel = formality.channel
            )
        )

        return RejectFormalityResult(
            formalityId = command.formalityId,
            status = FormalityStatus.REJECTED,
            reasonCode = command.reasonCode,
            reasonDescription = command.reasonDescription,
            frmSentAt = now
        )
    }
}

data class RejectFormalityCommand(
    val formalityId: UUID,
    val reasonCode: String,
    val reasonDescription: String,
    val reviewerUserId: UUID
)

data class RejectFormalityResult(
    val formalityId: UUID,
    val status: FormalityStatus,
    val reasonCode: String,
    val reasonDescription: String,
    val frmSentAt: OffsetDateTime
)
