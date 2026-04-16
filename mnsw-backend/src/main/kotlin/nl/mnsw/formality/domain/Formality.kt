package nl.mnsw.formality.domain

import nl.mnsw.formality.domain.payload.FormalityPayload
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Formality — aggregate root voor de domeinlaag.
 * Geen Spring- of JPA-annotaties — pure domeinlaag (hexagonale architectuur).
 *
 * Een formality is een verplichte melding conform EMSWe Richtlijn 2019/1239/EU.
 * Versioning via supersededBy: bij correctie wordt een nieuw record aangemaakt en
 * het oude record krijgt status SUPERSEDED. Immutable history — nooit overschrijven.
 */
data class Formality(
    val id: UUID,
    val visitId: UUID,
    val type: FormalityType,
    val version: Int,
    val status: FormalityStatus,
    val submitterId: UUID,
    val lrn: String?,
    val messageIdentifier: String,
    val submittedAt: OffsetDateTime,
    val supersededBy: UUID?,
    val channel: SubmissionChannel,
    val payload: FormalityPayload
)
