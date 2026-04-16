package nl.mnsw.visit.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * JPA-entiteit voor de visit tabel.
 * Gescheiden van de domeinlaag conform hexagonale architectuur.
 * Mapping naar domein-klasse Visit via FormalityMapper / VisitMapper.
 */
@Entity
@Table(name = "visit")
class VisitJpaEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "imo_number", nullable = false, length = 10)
    var imoNumber: String = "",

    @Column(name = "vessel_name", nullable = false)
    var vesselName: String = "",

    @Column(name = "vessel_flag", length = 3)
    var vesselFlag: String? = null,

    @Column(name = "port_locode", nullable = false, length = 10)
    var portLocode: String = "",

    @Column(name = "eta")
    var eta: OffsetDateTime? = null,

    @Column(name = "etd")
    var etd: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
)
