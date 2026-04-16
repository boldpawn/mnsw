package nl.mnsw.formality.domain.payload

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Sealed class hiërachie voor type-specifieke formality payloads.
 * Geen Spring- of JPA-annotaties — pure domainlaag.
 * Conform EMSWe MIG v2.0.1.
 */
sealed class FormalityPayload {

    /**
     * NOA — Notification of Arrival payload.
     * Conform EMSWe MIG v2.0.1 datacategorie "Notification of Arrival".
     */
    data class NoaPayload(
        val expectedArrival: OffsetDateTime,
        val lastPortLocode: String?,
        val nextPortLocode: String?,
        val purposeOfCall: String?,
        val personsOnBoard: Int?,
        val dangerousGoods: Boolean = false,
        val wasteDelivery: Boolean = false,
        val maxStaticDraught: BigDecimal?
    ) : FormalityPayload()

    /**
     * NOS — Notification of Sailing payload.
     * Conform EMSWe MIG v2.0.1 datacategorie "Notification of Sailing".
     */
    data class NosPayload(
        val actualSailing: OffsetDateTime,
        val nextPortLocode: String?,
        val destinationCountry: String?
    ) : FormalityPayload()

    /**
     * NOD — Notification of Departure payload.
     * Conform EMSWe MIG v2.0.1 datacategorie "Notification of Departure".
     * Niet te verwarren met NOS: NOD is de formele vertrekmelding vooraf, NOS bevestigt het feitelijke vertrek.
     */
    data class NodPayload(
        val expectedDeparture: OffsetDateTime,
        val nextPortLocode: String?,
        val destinationCountry: String?,
        val lastCargoOperations: OffsetDateTime?
    ) : FormalityPayload()

    /**
     * VID — Vessel Identification Documents payload.
     * Conform EMSWe MIG v2.0.1 datacategorie "Ship/Vessel Identification".
     */
    data class VidPayload(
        val certificateNationality: String?,
        val grossTonnage: BigDecimal?,
        val netTonnage: BigDecimal?,
        val deadweight: BigDecimal?,
        val lengthOverall: BigDecimal?,
        val shipType: String?,
        val callSign: String?,
        val mmsi: String?
    ) : FormalityPayload()

    /**
     * SID — Ship/Security Information Document (ISPS) payload.
     * Conform ISPS-regelgeving en EMSWe MIG v2.0.1 datacategorie "Security".
     * Bij ispsLevel >= 2 is designatedAuthority verplicht (domeinvalidatie in FormalityValidator).
     */
    data class SidPayload(
        val ispsLevel: Int,
        val last10Ports: List<PortEntry> = emptyList(),
        val securityDeclaration: String?,
        val shipToShipActivities: Boolean = false,
        val designatedAuthority: String?,
        val ssasActivated: Boolean = false
    ) : FormalityPayload()
}

/**
 * Een aanloophaven in de SID last_10_ports lijst.
 * Gebruikt in SidPayload — wordt geserialiseerd als JSONB in de database.
 */
data class PortEntry(
    val locode: String,
    val arrivalDate: LocalDate?,
    val departureDate: LocalDate?,
    val ispsLevel: Int?
)
