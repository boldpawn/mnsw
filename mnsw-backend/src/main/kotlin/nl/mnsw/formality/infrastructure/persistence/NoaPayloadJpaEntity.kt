package nl.mnsw.formality.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

/**
 * JPA-entiteit voor de noa_payload tabel.
 * Deelt PK met formality (formality_id is zowel PK als FK).
 * Conform EMSWe MIG v2.0.1 datacategorie "Notification of Arrival".
 */
@Entity
@Table(name = "noa_payload")
class NoaPayloadJpaEntity(

    @Id
    @Column(name = "formality_id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "formality_id")
    var formality: FormalityJpaEntity = FormalityJpaEntity(),

    @Column(name = "expected_arrival", nullable = false)
    var expectedArrival: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "last_port_locode", length = 10)
    var lastPortLocode: String? = null,

    @Column(name = "next_port_locode", length = 10)
    var nextPortLocode: String? = null,

    @Column(name = "purpose_of_call")
    var purposeOfCall: String? = null,

    @Column(name = "persons_on_board")
    var personsOnBoard: Int? = null,

    @Column(name = "dangerous_goods", nullable = false)
    var dangerousGoods: Boolean = false,

    @Column(name = "waste_delivery", nullable = false)
    var wasteDelivery: Boolean = false,

    @Column(name = "max_static_draught", precision = 5, scale = 2)
    var maxStaticDraught: BigDecimal? = null
)
