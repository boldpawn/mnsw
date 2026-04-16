package nl.mnsw.formality.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import nl.mnsw.auth.infrastructure.persistence.UserJpaEntity
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.visit.infrastructure.persistence.VisitJpaEntity
import java.time.OffsetDateTime
import java.util.UUID

/**
 * JPA-entiteit voor de formality tabel — supertype voor alle formality types.
 * Gescheiden van de domeinlaag conform hexagonale architectuur.
 *
 * Payload-relaties zijn @OneToOne LAZY — precies één payload-entiteit is aanwezig
 * afhankelijk van het type. De niet-relevante payload-associaties zijn null.
 * supersededBy is opgeslagen als UUID (geen FK-entiteit) om circulaire dependency te vermijden.
 */
@Entity
@Table(name = "formality")
class FormalityJpaEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false, updatable = false)
    var visit: VisitJpaEntity = VisitJpaEntity(),

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, columnDefinition = "formality_type")
    var type: FormalityType = FormalityType.NOA,

    @Column(name = "version", nullable = false)
    var version: Int = 1,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "formality_status")
    var status: FormalityStatus = FormalityStatus.SUBMITTED,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitter_id", nullable = false, updatable = false)
    var submitter: UserJpaEntity = UserJpaEntity(),

    @Column(name = "lrn")
    var lrn: String? = null,

    @Column(name = "message_identifier", nullable = false)
    var messageIdentifier: String = "",

    @Column(name = "submitted_at", nullable = false, updatable = false)
    var submittedAt: OffsetDateTime = OffsetDateTime.now(),

    /**
     * UUID van de opvolgende formality (bij correctie).
     * Opgeslagen als UUID — geen FK-entiteit om circulaire dependency te vermijden.
     * De FK constraint op DB-niveau (V3 migratie) garandeert referentiële integriteit.
     */
    @Column(name = "superseded_by")
    var supersededBy: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, columnDefinition = "submission_channel")
    var channel: SubmissionChannel = SubmissionChannel.WEB,

    // ===== Type-specifieke payload-relaties (precies één is aanwezig) =====

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "formality", optional = true)
    var noaPayload: NoaPayloadJpaEntity? = null,

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "formality", optional = true)
    var nosPayload: NosPayloadJpaEntity? = null,

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "formality", optional = true)
    var nodPayload: NodPayloadJpaEntity? = null,

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "formality", optional = true)
    var vidPayload: VidPayloadJpaEntity? = null,

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "formality", optional = true)
    var sidPayload: SidPayloadJpaEntity? = null
)
