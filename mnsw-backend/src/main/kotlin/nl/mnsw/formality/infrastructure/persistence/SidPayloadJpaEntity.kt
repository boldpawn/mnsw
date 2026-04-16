package nl.mnsw.formality.infrastructure.persistence

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import nl.mnsw.formality.domain.payload.PortEntry
import java.util.UUID

/**
 * JPA-entiteit voor de sid_payload tabel.
 * Deelt PK met formality (formality_id is zowel PK als FK).
 * Conform ISPS-regelgeving en EMSWe MIG v2.0.1 datacategorie "Security".
 *
 * last_10_ports is opgeslagen als JSONB String — geserialiseerd/gedeserialiseerd via PortEntryListConverter.
 * JSONB is hier gerechtvaardigd: de last_10_ports is een dynamische lijst van complexe havenobjecten.
 */
@Entity
@Table(name = "sid_payload")
class SidPayloadJpaEntity(

    @Id
    @Column(name = "formality_id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "formality_id")
    var formality: FormalityJpaEntity = FormalityJpaEntity(),

    @Column(name = "isps_level", nullable = false)
    var ispsLevel: Int = 1,

    /**
     * JSONB kolom: lijst van laatste 10 aanloophavens.
     * Geconverteerd via PortEntryListConverter (Jackson).
     */
    @Convert(converter = PortEntryListConverter::class)
    @Column(name = "last_10_ports", columnDefinition = "jsonb")
    var last10Ports: List<PortEntry> = emptyList(),

    @Column(name = "security_declaration", length = 50)
    var securityDeclaration: String? = null,

    @Column(name = "ship_to_ship_activities", nullable = false)
    var shipToShipActivities: Boolean = false,

    @Column(name = "designated_authority")
    var designatedAuthority: String? = null,

    @Column(name = "ssas_activated", nullable = false)
    var ssasActivated: Boolean = false
)

/**
 * JPA AttributeConverter voor JSONB serialisatie van List<PortEntry>.
 * Gebruikt Jackson voor JSON serialisatie/deserialisatie.
 * autoApply = false — expliciet aangeroepen via @Convert op het veld.
 */
@Converter(autoApply = false)
class PortEntryListConverter : AttributeConverter<List<PortEntry>, String> {

    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private val typeReference = object : TypeReference<List<PortEntry>>() {}

    override fun convertToDatabaseColumn(attribute: List<PortEntry>?): String? {
        if (attribute.isNullOrEmpty()) return null
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): List<PortEntry> {
        if (dbData.isNullOrBlank()) return emptyList()
        return objectMapper.readValue(dbData, typeReference)
    }
}
