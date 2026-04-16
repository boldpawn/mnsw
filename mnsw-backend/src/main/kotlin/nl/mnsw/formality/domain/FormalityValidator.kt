package nl.mnsw.formality.domain

import nl.mnsw.formality.domain.payload.FormalityPayload
import nl.mnsw.shared.exception.FieldError
import nl.mnsw.shared.exception.ValidationException
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Domeinvalidator voor alle EMSWe formality types conform EMSA MIG v2.0.1.
 *
 * Geen JPA- of HTTP-afhankelijkheden — pure domeinlogica (hexagonale architectuur).
 *
 * Validatielagen:
 *  1. Bean Validation (in REST-laag) — verplichte velden, formaatcontroles
 *  2. FormalityValidator (hier) — business rules conform MIG
 *  3. DB constraints — vangnet op databaseniveau
 *
 * Fail-all strategie: alle veldfouten worden verzameld en als één ValidationException gegooid
 * zodat de indiener alle problemen in één respons ontvangt.
 */
@Component
class FormalityValidator {

    // UN/LOCODE formaat: 2 hoofdletters (ISO 3166-1 alpha-2 landcode) + 3 alfanumerieke tekens
    // Voorbeelden: NLRTM (Rotterdam), DEHAM (Hamburg), GBFXT (Felixstowe)
    private val locodeRegex = Regex("^[A-Z]{2}[A-Z0-9]{3}$")

    // Roepletters (call sign): 3 tot 7 alfanumerieke tekens (ITU-R standaard)
    private val callSignRegex = Regex("^[A-Z0-9]{3,7}$")

    /**
     * Valideer een formality payload conform MIG-regels voor het gegeven type.
     *
     * Voert gedeelde validaties uit (portLocode, imoNumber) voor alle types,
     * gevolgd door type-specifieke validaties.
     *
     * @param type Het formality type (NOA, NOS, NOD, VID, SID)
     * @param payload De type-specifieke payload
     * @param visitImoNumber Het IMO-nummer van het schip uit de Visit
     * @param visitPortLocode Het UN/LOCODE van de haven uit de Visit
     * @throws ValidationException met alle veldfouten als de validatie mislukt
     */
    fun validate(
        type: FormalityType,
        payload: FormalityPayload,
        visitImoNumber: String,
        visitPortLocode: String
    ) {
        val errors = mutableListOf<FieldError>()

        // Gedeelde validaties — gelden voor ALLE formality types
        validatePortLocode(visitPortLocode, errors)
        validateImoNumber(visitImoNumber, errors)

        // Type-specifieke validaties via when op de sealed class
        when (payload) {
            is FormalityPayload.NoaPayload -> validateNoa(payload, errors)
            is FormalityPayload.NosPayload -> validateNos(payload, errors)
            is FormalityPayload.NodPayload -> validateNod(payload, errors)
            is FormalityPayload.VidPayload -> validateVid(payload, errors)
            is FormalityPayload.SidPayload -> validateSid(payload, errors)
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }

    /**
     * NOA-validatie conform EMSWe MIG:
     * - expectedArrival mag niet meer dan 24 uur geleden zijn
     * - personsOnBoard >= 0 indien opgegeven
     * - maxStaticDraught > 0 en < 50 meter indien opgegeven
     */
    private fun validateNoa(payload: FormalityPayload.NoaPayload, errors: MutableList<FieldError>) {
        val earliestAllowed = OffsetDateTime.now().minusHours(24)

        if (payload.expectedArrival.isBefore(earliestAllowed)) {
            errors.add(
                FieldError(
                    field = "payload.expectedArrival",
                    code = "NOA_ARRIVAL_TOO_OLD",
                    message = "Verwachte aankomsttijd mag niet meer dan 24 uur in het verleden liggen"
                )
            )
        }

        payload.personsOnBoard?.let { pob ->
            if (pob < 0) {
                errors.add(
                    FieldError(
                        field = "payload.personsOnBoard",
                        code = "NOA_INVALID_PERSONS_ON_BOARD",
                        message = "Aantal personen aan boord mag niet negatief zijn"
                    )
                )
            }
        }

        payload.maxStaticDraught?.let { draught ->
            if (draught <= BigDecimal.ZERO || draught >= BigDecimal("50")) {
                errors.add(
                    FieldError(
                        field = "payload.maxStaticDraught",
                        code = "NOA_INVALID_MAX_STATIC_DRAUGHT",
                        message = "Maximale statische diepgang moet groter dan 0 en kleiner dan 50 meter zijn"
                    )
                )
            }
        }
    }

    /**
     * NOS-validatie conform EMSWe MIG:
     * - actualSailing mag niet meer dan 48 uur in de toekomst liggen
     */
    private fun validateNos(payload: FormalityPayload.NosPayload, errors: MutableList<FieldError>) {
        val latestAllowed = OffsetDateTime.now().plusHours(48)
        if (payload.actualSailing.isAfter(latestAllowed)) {
            errors.add(
                FieldError(
                    field = "payload.actualSailing",
                    code = "NOS_SAILING_TOO_FAR_IN_FUTURE",
                    message = "Feitelijk vertrektijdstip mag niet meer dan 48 uur in de toekomst liggen"
                )
            )
        }
    }

    /**
     * NOD-validatie conform EMSWe MIG:
     * - expectedDeparture moet in de toekomst liggen
     */
    private fun validateNod(payload: FormalityPayload.NodPayload, errors: MutableList<FieldError>) {
        if (!payload.expectedDeparture.isAfter(OffsetDateTime.now())) {
            errors.add(
                FieldError(
                    field = "payload.expectedDeparture",
                    code = "NOD_DEPARTURE_IN_PAST",
                    message = "Verwacht vertrektijdstip moet in de toekomst liggen"
                )
            )
        }
    }

    /**
     * VID-validatie conform EMSWe MIG:
     * - mmsi: exact 9 cijfers indien opgegeven (ITU-R M.585 standaard)
     * - callSign: 3-7 alfanumerieke tekens indien opgegeven
     */
    private fun validateVid(payload: FormalityPayload.VidPayload, errors: MutableList<FieldError>) {
        payload.mmsi?.let { mmsi ->
            if (!mmsi.matches(Regex("^\\d{9}$"))) {
                errors.add(
                    FieldError(
                        field = "payload.mmsi",
                        code = "VID_INVALID_MMSI",
                        message = "MMSI-nummer moet exact 9 cijfers bevatten"
                    )
                )
            }
        }

        payload.callSign?.let { callSign ->
            if (!callSign.uppercase().matches(callSignRegex)) {
                errors.add(
                    FieldError(
                        field = "payload.callSign",
                        code = "VID_INVALID_CALLSIGN",
                        message = "Roepletters moeten 3 tot 7 alfanumerieke tekens bevatten"
                    )
                )
            }
        }
    }

    /**
     * SID-validatie conform EMSWe MIG / ISPS:
     * - ispsLevel moet 1, 2 of 3 zijn (conform ISPS Code definitie)
     * - Bij ispsLevel >= 2: designatedAuthority is verplicht
     */
    private fun validateSid(payload: FormalityPayload.SidPayload, errors: MutableList<FieldError>) {
        if (payload.ispsLevel !in 1..3) {
            errors.add(
                FieldError(
                    field = "payload.ispsLevel",
                    code = "SID_INVALID_ISPS_LEVEL",
                    message = "ISPS-niveau moet 1, 2 of 3 zijn"
                )
            )
        }

        if (payload.ispsLevel in 2..3 && payload.designatedAuthority.isNullOrBlank()) {
            errors.add(
                FieldError(
                    field = "payload.designatedAuthority",
                    code = "SID_DESIGNATED_AUTHORITY_REQUIRED",
                    message = "Bevoegde ISPS-autoriteit is verplicht bij ISPS-niveau 2 of 3"
                )
            )
        }
    }

    // ===== Gedeelde validaties =====

    /**
     * Valideer het UN/LOCODE formaat.
     *
     * Formaat: 2 hoofdletters (ISO 3166-1 alpha-2 landcode) + 3 alfanumerieke tekens (plaatscode).
     * Bron: UN/LOCODE standaard (UNECE Trade Facilitation Recommendation 16)
     */
    private fun validatePortLocode(locode: String, errors: MutableList<FieldError>) {
        if (!locode.matches(locodeRegex)) {
            errors.add(
                FieldError(
                    field = "visit.portLocode",
                    code = "INVALID_LOCODE",
                    message = "Port LOCODE moet het formaat hebben van 2 letters + 3 alfanumerieke tekens (bijv. NLRTM)"
                )
            )
        }
    }

    /**
     * Valideer een IMO-scheepsnummer via het officiële checksum-algoritme.
     *
     * IMO-nummers bestaan uit 7 cijfers. Het algoritme (IMO resolutie A.600(15)):
     * - De eerste 6 cijfers worden elk vermenigvuldigd met hun positiegewicht (7, 6, 5, 4, 3, 2)
     * - De som van de producten modulo 10 geeft het 7e cijfer (checkdigit)
     */
    private fun validateImoNumber(imo: String, errors: MutableList<FieldError>) {
        if (!imo.matches(Regex("^\\d{7}$"))) {
            errors.add(
                FieldError(
                    field = "visit.imoNumber",
                    code = "INVALID_IMO",
                    message = "IMO-nummer moet exact 7 cijfers bevatten"
                )
            )
            return  // Geen zinvolle checksum-berekening mogelijk zonder 7 cijfers
        }

        val digits = imo.map { it.digitToInt() }
        val weights = intArrayOf(7, 6, 5, 4, 3, 2)
        val checksum = digits.take(6).zip(weights.toList()).sumOf { (digit, weight) -> digit * weight }
        val expectedCheckDigit = checksum % 10

        if (digits[6] != expectedCheckDigit) {
            errors.add(
                FieldError(
                    field = "visit.imoNumber",
                    code = "INVALID_IMO",
                    message = "IMO-nummer $imo heeft een ongeldige checksum (verwacht checkdigit: $expectedCheckDigit)"
                )
            )
        }
    }
}
