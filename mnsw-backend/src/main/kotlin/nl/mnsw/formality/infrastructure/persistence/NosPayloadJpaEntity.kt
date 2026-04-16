package nl.mnsw.formality.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * JPA-entiteit voor de nos_payload tabel.
 * Deelt PK met formality (formality_id is zowel PK als FK).
 * Conform EMSWe MIG v2.0.1 datacategorie "Notification of Sailing".
 */
@Entity
@Table(name = "nos_payload")
class NosPayloadJpaEntity(

    @Id
    @Column(name = "formality_id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "formality_id")
    var formality: FormalityJpaEntity = FormalityJpaEntity(),

    @Column(name = "actual_sailing", nullable = false)
    var actualSailing: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "next_port_locode", length = 10)
    var nextPortLocode: String? = null,

    @Column(name = "destination_country", length = 3)
    var destinationCountry: String? = null
)
