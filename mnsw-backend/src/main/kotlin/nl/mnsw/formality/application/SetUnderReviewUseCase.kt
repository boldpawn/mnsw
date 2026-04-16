package nl.mnsw.formality.application

import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.infrastructure.persistence.FormalityRepository
import nl.mnsw.auth.infrastructure.persistence.UserRepository
import nl.mnsw.shared.exception.FormalityNotFoundException
import nl.mnsw.shared.exception.UnauthorizedAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Use case om een formality op UNDER_REVIEW te zetten.
 * Toegestaan voor HAVENAUTORITEIT (eigen haven) en ADMIN.
 *
 * Autorisatie: formality.visit.portLocode == reviewer.portLocode (of ADMIN).
 * Status: SUBMITTED -> UNDER_REVIEW.
 */
@Service
@Transactional
class SetUnderReviewUseCase(
    private val formalityRepository: FormalityRepository,
    private val userRepository: UserRepository
) {

    fun execute(command: SetUnderReviewCommand): SetUnderReviewResult {
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

        // 3. Zet status op UNDER_REVIEW
        formality.status = FormalityStatus.UNDER_REVIEW
        formalityRepository.save(formality)

        return SetUnderReviewResult(
            formalityId = command.formalityId,
            status = FormalityStatus.UNDER_REVIEW
        )
    }
}

data class SetUnderReviewCommand(
    val formalityId: UUID,
    val reviewerUserId: UUID
)

data class SetUnderReviewResult(
    val formalityId: UUID,
    val status: FormalityStatus
)
