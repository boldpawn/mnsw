package nl.mnsw.formality.application

import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.formality.infrastructure.persistence.FrmStatus
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Event dat gepubliceerd wordt na succesvolle opslag van een formality.
 * Wordt gepubliceerd after-commit via @TransactionalEventListener.
 * Pulsar producer luistert op dit event (geïmplementeerd in B08).
 */
data class FormalitySubmittedEvent(
    val formalityId: UUID,
    val visitId: UUID,
    val type: FormalityType,
    val portLocode: String,
    val submittedAt: OffsetDateTime,
    val channel: SubmissionChannel,
    val submitterId: UUID,
    val messageIdentifier: String
)

/**
 * Event dat gepubliceerd wordt wanneer een FRM-response aangemaakt is.
 * Wordt gepubliceerd after-commit via @TransactionalEventListener.
 * Pulsar producer stuurt de FRM terug naar de indiener (voor RIM-kanaal).
 */
data class FrmResponseCreatedEvent(
    val formalityId: UUID,
    val frmStatus: FrmStatus,
    val reasonCode: String?,
    val reasonDescription: String?,
    val channel: SubmissionChannel
)
