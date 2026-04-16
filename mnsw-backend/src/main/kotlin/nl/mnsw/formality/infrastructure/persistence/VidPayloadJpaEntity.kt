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
import java.util.UUID

/**
 * JPA-entiteit voor de vid_payload tabel.
 * Deelt PK met formality (formality_id is zowel PK als FK).
 * Conform EMSWe MIG v2.0.1 datacategorie "Ship/Vessel Identification".
 */
@Entity
@Table(name = "vid_payload")
class VidPayloadJpaEntity(

    @Id
    @Column(name = "formality_id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "formality_id")
    var formality: FormalityJpaEntity = FormalityJpaEntity(),

    @Column(name = "certificate_nationality")
    var certificateNationality: String? = null,

    @Column(name = "gross_tonnage", precision = 10, scale = 2)
    var grossTonnage: BigDecimal? = null,

    @Column(name = "net_tonnage", precision = 10, scale = 2)
    var netTonnage: BigDecimal? = null,

    @Column(name = "deadweight", precision = 10, scale = 2)
    var deadweight: BigDecimal? = null,

    @Column(name = "length_overall", precision = 8, scale = 2)
    var lengthOverall: BigDecimal? = null,

    @Column(name = "ship_type", length = 100)
    var shipType: String? = null,

    @Column(name = "call_sign", length = 20)
    var callSign: String? = null,

    @Column(name = "mmsi", length = 15)
    var mmsi: String? = null
)
