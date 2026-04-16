# Spec: Data Model

## Domeinelementen

### Formality (aggregate root)

De centrale entiteit. Elke ingediende formality heeft:

| Veld | Type | Verplicht | Beschrijving |
|------|------|-----------|-------------|
| id | UUID | ja | Primaire sleutel, gegenereerd door MNSW |
| visit_id | UUID | ja | FK naar Visit |
| type | FormalityType | ja | NOA, NOS, NOD, VID of SID |
| version | Int | ja | Begint bij 1, opgehoogd bij correctie |
| status | FormalityStatus | ja | Zie statusdiagram |
| submitter_id | UUID | ja | FK naar app_user |
| lrn | String | nee | Local Reference Number van indiener |
| message_identifier | String | ja | MAI berichtidentificatie |
| submitted_at | Instant | ja | Tijdstip indiening |
| superseded_by | UUID | nee | FK naar nieuwere versie bij correctie |
| channel | SubmissionChannel | ja | WEB of RIM |

### FormalityStatus (enum)

```
SUBMITTED     → initieel na indiening
UNDER_REVIEW  → havenautoriteit beoordeelt
ACCEPTED      → goedgekeurd door havenautoriteit
REJECTED      → afgewezen, FRM met redenen gestuurd
SUPERSEDED    → vervangen door nieuwere versie (correctie)
```

Statusovergangen:
```
SUBMITTED → UNDER_REVIEW (havenautoriteit opent)
SUBMITTED → REJECTED     (directe afwijzing bij validatiefout)
UNDER_REVIEW → ACCEPTED
UNDER_REVIEW → REJECTED
ACCEPTED/REJECTED/UNDER_REVIEW → SUPERSEDED (bij correctie)
```

### Visit

| Veld | Type | Verplicht | Beschrijving |
|------|------|-----------|-------------|
| id | UUID | ja | Primaire sleutel |
| imo_number | String | ja | IMO scheepsnummer (7 cijfers) |
| vessel_name | String | ja | Naam van het schip |
| vessel_flag | String | nee | ISO 3166-1 alpha-3 vlagstaat |
| port_locode | String | ja | UN/LOCODE van haven (bijv. NLRTM) |
| eta | Instant | nee | Verwachte aankomsttijd |
| etd | Instant | nee | Verwachte vertrektijd |
| created_at | Instant | ja | Aanmaaktijdstip |

Visit wordt aangemaakt bij de eerste NOA voor een schip+haven+periode combinatie. Alle volgende formalities voor hetzelfde bezoek verwijzen naar dezelfde Visit via visit_id.

### NOA Payload

Conform EMSWe MIG v2.0.1 "Notification of Arrival":

| Veld | Type | Verplicht | Beschrijving |
|------|------|-----------|-------------|
| formality_id | UUID | ja | PK + FK |
| expected_arrival | Instant | ja | Verwachte aankomsttijd, mag niet in verleden |
| last_port_locode | String | nee | UN/LOCODE vorige haven |
| next_port_locode | String | nee | UN/LOCODE volgende haven |
| purpose_of_call | String | nee | Doel van havenbezoek |
| persons_on_board | Int | nee | Aantal personen aan boord |
| dangerous_goods | Boolean | nee | Gevaarlijke stoffen aan boord |
| waste_delivery | Boolean | nee | Afvalafgifte gewenst |
| max_static_draught | Decimal | nee | Maximale statische diepgang in meters |

### NOS Payload

Conform EMSWe MIG v2.0.1 "Notification of Sailing":

| Veld | Type | Verplicht | Beschrijving |
|------|------|-----------|-------------|
| formality_id | UUID | ja | PK + FK |
| actual_sailing | Instant | ja | Feitelijk vertrektijdstip |
| next_port_locode | String | nee | Volgende bestemming |
| destination_country | String | nee | ISO 3166-1 alpha-3 |

### NOD Payload

Conform EMSWe MIG v2.0.1 "Notification of Departure":

| Veld | Type | Verplicht | Beschrijving |
|------|------|-----------|-------------|
| formality_id | UUID | ja | PK + FK |
| expected_departure | Instant | ja | Geplande vertrektijd |
| next_port_locode | String | nee | Volgende bestemming |
| destination_country | String | nee | ISO 3166-1 alpha-3 |
| last_cargo_operations | Instant | nee | Einde laatste laad-/losoperatie |

### VID Payload

Conform EMSWe MIG v2.0.1 "Vessel Identification":

| Veld | Type | Verplicht | Beschrijving |
|------|------|-----------|-------------|
| formality_id | UUID | ja | PK + FK |
| certificate_nationality | String | nee | Nationaliteitscertificaat |
| gross_tonnage | Decimal | nee | Brutotonnemaat |
| net_tonnage | Decimal | nee | Nettotonnemaat |
| deadweight | Decimal | nee | Draagvermogen (DWT) |
| length_overall | Decimal | nee | Lengte over alles (LOA) in meters |
| ship_type | String | nee | Scheepstype (bijv. "BULK CARRIER") |
| call_sign | String | nee | Roepletters |
| mmsi | String | nee | MMSI-nummer (9 cijfers) |

### SID Payload (ISPS Security)

Conform ISPS-regelgeving en EMSWe MIG v2.0.1 "Security":

| Veld | Type | Verplicht | Beschrijving |
|------|------|-----------|-------------|
| formality_id | UUID | ja | PK + FK |
| isps_level | Int | ja | ISPS-niveau: 1, 2 of 3 |
| last_10_ports | JSONB | nee | Lijst van laatste 10 aanloophavens |
| security_declaration | String | nee | Beveiligingsverklaring type |
| ship_to_ship_activities | Boolean | nee | Schip-tot-schip activiteiten gehad |
| designated_authority | String | nee | Bevoegde ISPS-autoriteit |
| ssas_activated | Boolean | nee | SSAS geactiveerd geweest |

*Noot: last_10_ports is JSONB omdat het een dynamische lijst is van complexe poortobjecten, wat beter past dan een aparte join-tabel voor deze leesintensieve query.*

### FRM Response

| Veld | Type | Verplicht | Beschrijving |
|------|------|-----------|-------------|
| id | UUID | ja | PK |
| formality_id | UUID | ja | FK naar formality |
| status | FrmStatus | ja | ACCEPTED, REJECTED, UNDER_REVIEW |
| reason_code | String | nee | EMSWe foutcode bij REJECTED |
| reason_description | String | nee | Nederlandse omschrijving |
| sent_at | Instant | nee | Tijdstip verzending FRM |
| channel | SubmissionChannel | ja | Terugkanaal: WEB of RIM |

### User

| Veld | Type | Verplicht | Beschrijving |
|------|------|-----------|-------------|
| id | UUID | ja | PK |
| email | String | ja | Uniek, gebruikt als gebruikersnaam |
| password_hash | String | ja | BCrypt hash |
| full_name | String | ja | Volledige naam |
| role | UserRole | ja | Enum rol |
| port_locode | String | cond. | Verplicht voor HAVENAUTORITEIT |
| active | Boolean | ja | Actief/geblokkeerd |
| created_at | Instant | ja | Aanmaaktijdstip |

---

## Kotlin Domein Sealed Class

```kotlin
sealed class FormalityPayload {
    data class NoaPayload(
        val expectedArrival: Instant,
        val lastPortLocode: String?,
        val nextPortLocode: String?,
        val purposeOfCall: String?,
        val personsOnBoard: Int?,
        val dangerousGoods: Boolean = false,
        val wasteDelivery: Boolean = false,
        val maxStaticDraught: BigDecimal?
    ) : FormalityPayload()

    data class NosPayload(
        val actualSailing: Instant,
        val nextPortLocode: String?,
        val destinationCountry: String?
    ) : FormalityPayload()

    data class NodPayload(
        val expectedDeparture: Instant,
        val nextPortLocode: String?,
        val destinationCountry: String?,
        val lastCargoOperations: Instant?
    ) : FormalityPayload()

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

    data class SidPayload(
        val ispsLevel: Int,
        val last10Ports: List<PortCall>?,
        val securityDeclaration: String?,
        val shipToShipActivities: Boolean = false,
        val designatedAuthority: String?,
        val ssasActivated: Boolean = false
    ) : FormalityPayload()
}

data class PortCall(
    val locode: String,
    val arrival: Instant?,
    val departure: Instant?,
    val ispsLevel: Int?
)
```

---

## Flyway Migratiebestanden

Volgorde:
```
V1__create_visit_table.sql
V2__create_formality_table.sql
V3__create_noa_payload_table.sql
V4__create_nos_payload_table.sql
V5__create_nod_payload_table.sql
V6__create_vid_payload_table.sql
V7__create_sid_payload_table.sql
V8__create_frm_response_table.sql
V9__create_app_user_table.sql
V10__create_indexes.sql
```

Locatie: `src/main/resources/db/migration/`

---

## Indexen

```sql
-- Primaire queries geoptimaliseerd:
CREATE INDEX idx_formality_visit_id ON formality(visit_id);
CREATE INDEX idx_formality_submitter_id ON formality(submitter_id);
CREATE INDEX idx_formality_status ON formality(status);
CREATE INDEX idx_formality_type ON formality(type);
CREATE INDEX idx_formality_message_identifier ON formality(message_identifier);
CREATE INDEX idx_visit_port_locode ON visit(port_locode);
CREATE INDEX idx_visit_imo_number ON visit(imo_number);
CREATE INDEX idx_app_user_email ON app_user(email);
```
