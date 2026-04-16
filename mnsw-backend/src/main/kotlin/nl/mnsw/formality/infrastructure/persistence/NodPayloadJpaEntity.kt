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
 * JPA-entiteit voor de nod_payload tabel.
 * Deelt PK met formality (formality_id is zowel PK als FK).
 * Conform EMSWe MIG v2.0.1 datacategorie "Notification of Departure".
 * Niet te verwarren met NOS: NOD is de formele vertrekmelding vooraf, NOS bevestigt het feitelijke vertrek.
 */
@Entity
@Table(name = "nod_payload")
class NodPayloadJpaEntity(

    @Id
    @Column(name = "formality_id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "formality_id")
    var formality: FormalityJpaEntity = FormalityJpaEntity(),

    @Column(name = "expected_departure", nullable = false)
    var expectedDeparture: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "next_port_locode", length = 10)
    var nextPortLocode: String? = null,

    @Column(name = "destination_country", length = 3)
    var destinationCountry: String? = null,

    @Column(name = "last_cargo_operations")
    var lastCargoOperations: OffsetDateTime? = null
)
