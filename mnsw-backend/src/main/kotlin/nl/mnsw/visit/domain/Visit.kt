package nl.mnsw.visit.domain

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Visit — havenbezoek.
 * Centrale aggregaat waaronder alle formalities van één havenbezoek worden gegroepeerd.
 * Wordt aangemaakt bij de eerste NOA voor een schip+haven+periode combinatie.
 * Geen Spring- of JPA-annotaties — pure domeinlaag.
 */
data class Visit(
    val id: UUID,
    val imoNumber: String,
    val vesselName: String,
    val vesselFlag: String?,
    val portLocode: String,
    val eta: OffsetDateTime?,
    val etd: OffsetDateTime?,
    val createdAt: OffsetDateTime
)
