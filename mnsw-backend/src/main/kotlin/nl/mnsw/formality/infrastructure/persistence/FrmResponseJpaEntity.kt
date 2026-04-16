package nl.mnsw.formality.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import nl.mnsw.formality.domain.SubmissionChannel
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Status van een FRM (Formality Response Message).
 * FRM is een uitgaand antwoord — GEEN inkomend formality type.
 */
enum class FrmStatus {
    ACCEPTED,
    REJECTED,
    UNDER_REVIEW
}

/**
 * JPA-entiteit voor de frm_response tabel.
 * Opgeslagen als uitkomend antwoord na beoordeling door havenautoriteit.
 * Gescheiden van de domeinlaag conform hexagonale architectuur.
 */
@Entity
@Table(name = "frm_response")
class FrmResponseJpaEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "formality_id", nullable = false, updatable = false)
    var formality: FormalityJpaEntity = FormalityJpaEntity(),

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "frm_status")
    var status: FrmStatus = FrmStatus.UNDER_REVIEW,

    @Column(name = "reason_code", length = 50)
    var reasonCode: String? = null,

    @Column(name = "reason_description", columnDefinition = "text")
    var reasonDescription: String? = null,

    @Column(name = "sent_at")
    var sentAt: OffsetDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, columnDefinition = "submission_channel")
    var channel: SubmissionChannel = SubmissionChannel.WEB
)
