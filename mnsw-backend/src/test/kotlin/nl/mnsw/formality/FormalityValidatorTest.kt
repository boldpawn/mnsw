package nl.mnsw.formality

import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.FormalityValidator
import nl.mnsw.formality.domain.payload.FormalityPayload
import nl.mnsw.shared.exception.ValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Unit tests voor FormalityValidator.
 *
 * Test ELKE validatieregel: happy path + foutpad.
 * Conform EMSWe MIG v2.0.1 en de foutcodes gedefinieerd in de backend spec.
 *
 * Testconventie: should{Actie}When{Conditie}
 * Geldige IMO voor tests: 9074729 (checksum klopt: 9×7+0×6+7×5+4×4+7×3+2×2 = 63+0+35+16+21+4=139, 139%10=9 ✓)
 */
class FormalityValidatorTest {

    private val validator = FormalityValidator()

    // ===== Geldige testdata =====
    private val validPortLocode = "NLRTM"
    private val validImoNumber = "9074729"  // Checksum: 9×7+0×6+7×5+4×4+7×3+2×2=139, 139%10=9 ✓

    private fun validNoaPayload(expectedArrival: OffsetDateTime = OffsetDateTime.now().plusHours(2)) =
        FormalityPayload.NoaPayload(
            expectedArrival = expectedArrival,
            lastPortLocode = null,
            nextPortLocode = null,
            purposeOfCall = null,
            personsOnBoard = null,
            dangerousGoods = false,
            wasteDelivery = false,
            maxStaticDraught = null
        )

    private fun validNosPayload(actualSailing: OffsetDateTime = OffsetDateTime.now().minusHours(1)) =
        FormalityPayload.NosPayload(
            actualSailing = actualSailing,
            nextPortLocode = null,
            destinationCountry = null
        )

    private fun validNodPayload(expectedDeparture: OffsetDateTime = OffsetDateTime.now().plusHours(6)) =
        FormalityPayload.NodPayload(
            expectedDeparture = expectedDeparture,
            nextPortLocode = null,
            destinationCountry = null,
            lastCargoOperations = null
        )

    private fun validVidPayload() =
        FormalityPayload.VidPayload(
            certificateNationality = null,
            grossTonnage = null,
            netTonnage = null,
            deadweight = null,
            lengthOverall = null,
            shipType = null,
            callSign = null,
            mmsi = null
        )

    private fun validSidPayload(ispsLevel: Int = 1, designatedAuthority: String? = null) =
        FormalityPayload.SidPayload(
            ispsLevel = ispsLevel,
            last10Ports = emptyList(),
            securityDeclaration = null,
            shipToShipActivities = false,
            designatedAuthority = designatedAuthority,
            ssasActivated = false
        )

    // ===== Gedeelde validaties: Port LOCODE =====

    @Test
    fun `shouldPassWhenPortLocodeIsValid`() {
        assertDoesNotThrow {
            validator.validate(FormalityType.NOA, validNoaPayload(), validImoNumber, validPortLocode)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["nlrtm", "NL", "NLRTM1", "NL-RTM", "123AB", "", "  "])
    fun `shouldFailWithInvalidLocodeWhenPortLocodeFormatIsWrong`(invalidLocode: String) {
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOA, validNoaPayload(), validImoNumber, invalidLocode)
        }
        val codes = exception.errors.map { it.code }
        assertTrue(codes.contains("INVALID_LOCODE"), "Expected INVALID_LOCODE but got: $codes")
    }

    @ParameterizedTest
    @ValueSource(strings = ["NLRTM", "DEHAM", "GBFXT", "FRBOY", "US1AB", "AA123"])
    fun `shouldPassWhenPortLocodeMatchesUnLocodeFormat`(validLocode: String) {
        assertDoesNotThrow {
            validator.validate(FormalityType.NOA, validNoaPayload(), validImoNumber, validLocode)
        }
    }

    // ===== Gedeelde validaties: IMO-nummer =====

    @Test
    fun `shouldPassWhenImoNumberIsValid`() {
        assertDoesNotThrow {
            validator.validate(FormalityType.NOA, validNoaPayload(), validImoNumber, validPortLocode)
        }
    }

    @ParameterizedTest
    // IMO numbers where the checkdigit does NOT match the computed checksum:
    // 9999999: sum=243, 243%10=3, but digit7=9 → invalid
    // 9355058: sum=136, 136%10=6, but digit7=8 → invalid
    // 1111111: sum=27,  27%10=7,  but digit7=1 → invalid
    @ValueSource(strings = ["9999999", "9355058", "1111111"])
    fun `shouldFailWithInvalidImoWhenImoChecksumIsWrong`(invalidImo: String) {
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOA, validNoaPayload(), invalidImo, validPortLocode)
        }
        val codes = exception.errors.map { it.code }
        assertTrue(codes.contains("INVALID_IMO"), "Expected INVALID_IMO but got: $codes")
    }

    @ParameterizedTest
    @ValueSource(strings = ["123456", "12345678", "ABC1234", ""])
    fun `shouldFailWithInvalidImoWhenImoDoesNotHaveSevenDigits`(invalidImo: String) {
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOA, validNoaPayload(), invalidImo, validPortLocode)
        }
        val codes = exception.errors.map { it.code }
        assertTrue(codes.contains("INVALID_IMO"), "Expected INVALID_IMO but got: $codes")
    }

    @ParameterizedTest
    // IMO numbers with correct checksums:
    // 9074729: sum=139, 139%10=9, digit7=9 ✓
    // 8814275: sum=145, 145%10=5, digit7=5 ✓
    // 1234567: sum=77,  77%10=7,  digit7=7 ✓
    @ValueSource(strings = ["9074729", "8814275", "1234567"])
    fun `shouldPassWhenImoNumberHasValidChecksum`(validImo: String) {
        assertDoesNotThrow {
            validator.validate(FormalityType.NOA, validNoaPayload(), validImo, validPortLocode)
        }
    }

    // ===== NOA validaties =====

    @Test
    fun `shouldPassWhenNoaArrivalIsInFuture`() {
        val payload = validNoaPayload(expectedArrival = OffsetDateTime.now().plusHours(6))
        assertDoesNotThrow {
            validator.validate(FormalityType.NOA, payload, validImoNumber, validPortLocode)
        }
    }

    @Test
    fun `shouldPassWhenNoaArrivalIsWithin24HoursAgo`() {
        val payload = validNoaPayload(expectedArrival = OffsetDateTime.now().minusHours(23))
        assertDoesNotThrow {
            validator.validate(FormalityType.NOA, payload, validImoNumber, validPortLocode)
        }
    }

    @Test
    fun `shouldFailWithNoaArrivalTooOldWhenArrivalIsMoreThan24HoursAgo`() {
        val payload = validNoaPayload(expectedArrival = OffsetDateTime.now().minusHours(25))
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOA, payload, validImoNumber, validPortLocode)
        }
        assertEquals("NOA_ARRIVAL_TOO_OLD", exception.errors.first().code)
        assertEquals("payload.expectedArrival", exception.errors.first().field)
    }

    @Test
    fun `shouldFailWhenNoaPersonsOnBoardIsNegative`() {
        val payload = validNoaPayload().copy(personsOnBoard = -1)
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOA, payload, validImoNumber, validPortLocode)
        }
        val codes = exception.errors.map { it.code }
        assertTrue(codes.contains("NOA_INVALID_PERSONS_ON_BOARD"))
    }

    @Test
    fun `shouldPassWhenNoaPersonsOnBoardIsZero`() {
        val payload = validNoaPayload().copy(personsOnBoard = 0)
        assertDoesNotThrow {
            validator.validate(FormalityType.NOA, payload, validImoNumber, validPortLocode)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["0.0", "50.0", "-1.0", "100.0"])
    fun `shouldFailWhenNoaMaxStaticDraughtIsOutOfBounds`(draughtStr: String) {
        val payload = validNoaPayload().copy(maxStaticDraught = BigDecimal(draughtStr))
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOA, payload, validImoNumber, validPortLocode)
        }
        val codes = exception.errors.map { it.code }
        assertTrue(codes.contains("NOA_INVALID_MAX_STATIC_DRAUGHT"), "Expected NOA_INVALID_MAX_STATIC_DRAUGHT but got: $codes")
    }

    @ParameterizedTest
    @ValueSource(strings = ["0.1", "1.0", "11.50", "25.0", "49.99"])
    fun `shouldPassWhenNoaMaxStaticDraughtIsInValidRange`(draughtStr: String) {
        val payload = validNoaPayload().copy(maxStaticDraught = BigDecimal(draughtStr))
        assertDoesNotThrow {
            validator.validate(FormalityType.NOA, payload, validImoNumber, validPortLocode)
        }
    }

    // ===== NOS validaties =====

    @Test
    fun `shouldPassWhenNosSailingIsInPast`() {
        val payload = validNosPayload(actualSailing = OffsetDateTime.now().minusHours(2))
        assertDoesNotThrow {
            validator.validate(FormalityType.NOS, payload, validImoNumber, validPortLocode)
        }
    }

    @Test
    fun `shouldPassWhenNosSailingIsWithin48HoursInFuture`() {
        val payload = validNosPayload(actualSailing = OffsetDateTime.now().plusHours(47))
        assertDoesNotThrow {
            validator.validate(FormalityType.NOS, payload, validImoNumber, validPortLocode)
        }
    }

    @Test
    fun `shouldFailWithNosSailingTooFarInFutureWhenSailingIsMoreThan48HoursAhead`() {
        val payload = validNosPayload(actualSailing = OffsetDateTime.now().plusHours(49))
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOS, payload, validImoNumber, validPortLocode)
        }
        assertEquals("NOS_SAILING_TOO_FAR_IN_FUTURE", exception.errors.first().code)
        assertEquals("payload.actualSailing", exception.errors.first().field)
    }

    // ===== NOD validaties =====

    @Test
    fun `shouldPassWhenNodExpectedDepartureIsInFuture`() {
        val payload = validNodPayload(expectedDeparture = OffsetDateTime.now().plusHours(12))
        assertDoesNotThrow {
            validator.validate(FormalityType.NOD, payload, validImoNumber, validPortLocode)
        }
    }

    @Test
    fun `shouldFailWithNodDepartureInPastWhenExpectedDepartureIsInPast`() {
        val payload = validNodPayload(expectedDeparture = OffsetDateTime.now().minusHours(1))
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOD, payload, validImoNumber, validPortLocode)
        }
        assertEquals("NOD_DEPARTURE_IN_PAST", exception.errors.first().code)
        assertEquals("payload.expectedDeparture", exception.errors.first().field)
    }

    @Test
    fun `shouldFailWithNodDepartureInPastWhenExpectedDepartureIsNow`() {
        // OffsetDateTime.now() is niet in de toekomst — grenswaarde test
        val pastDeparture = OffsetDateTime.now().minusSeconds(1)
        val payload = validNodPayload(expectedDeparture = pastDeparture)
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOD, payload, validImoNumber, validPortLocode)
        }
        val codes = exception.errors.map { it.code }
        assertTrue(codes.contains("NOD_DEPARTURE_IN_PAST"))
    }

    // ===== VID validaties =====

    @Test
    fun `shouldPassWhenVidHasNoOptionalFields`() {
        assertDoesNotThrow {
            validator.validate(FormalityType.VID, validVidPayload(), validImoNumber, validPortLocode)
        }
    }

    @Test
    fun `shouldPassWhenVidMmsiIsExactly9Digits`() {
        val payload = validVidPayload().copy(mmsi = "244123456")
        assertDoesNotThrow {
            validator.validate(FormalityType.VID, payload, validImoNumber, validPortLocode)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["12345678", "1234567890", "ABCDEFGHI", "244-12-345"])
    fun `shouldFailWithVidInvalidMmsiWhenMmsiIsNotExactly9Digits`(invalidMmsi: String) {
        val payload = validVidPayload().copy(mmsi = invalidMmsi)
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.VID, payload, validImoNumber, validPortLocode)
        }
        val codes = exception.errors.map { it.code }
        assertTrue(codes.contains("VID_INVALID_MMSI"), "Expected VID_INVALID_MMSI but got: $codes")
    }

    @ParameterizedTest
    @ValueSource(strings = ["PBZK", "ABC", "PH12345", "A1B2C3D"])
    fun `shouldPassWhenVidCallSignIs3To7AlphanumericChars`(validCallSign: String) {
        val payload = validVidPayload().copy(callSign = validCallSign)
        assertDoesNotThrow {
            validator.validate(FormalityType.VID, payload, validImoNumber, validPortLocode)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["AB", "ABCDEFGH", "PB-ZK", "pb zk", "PB_ZK"])
    fun `shouldFailWithVidInvalidCallsignWhenCallSignIsInvalid`(invalidCallSign: String) {
        val payload = validVidPayload().copy(callSign = invalidCallSign)
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.VID, payload, validImoNumber, validPortLocode)
        }
        val codes = exception.errors.map { it.code }
        assertTrue(codes.contains("VID_INVALID_CALLSIGN"), "Expected VID_INVALID_CALLSIGN but got: $codes")
    }

    // ===== SID validaties =====

    @Test
    fun `shouldPassWhenSidIspsLevel1WithNoDesignatedAuthority`() {
        val payload = validSidPayload(ispsLevel = 1, designatedAuthority = null)
        assertDoesNotThrow {
            validator.validate(FormalityType.SID, payload, validImoNumber, validPortLocode)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3])
    fun `shouldPassWhenSidIspsLevelIsValid`(level: Int) {
        val authority = if (level >= 2) "Port Security Authority Rotterdam" else null
        val payload = validSidPayload(ispsLevel = level, designatedAuthority = authority)
        assertDoesNotThrow {
            validator.validate(FormalityType.SID, payload, validImoNumber, validPortLocode)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 4, -1, 99])
    fun `shouldFailWithSidInvalidIspsLevelWhenIspsLevelIsOutOfRange`(invalidLevel: Int) {
        val payload = validSidPayload(ispsLevel = invalidLevel, designatedAuthority = "Some Authority")
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.SID, payload, validImoNumber, validPortLocode)
        }
        val codes = exception.errors.map { it.code }
        assertTrue(codes.contains("SID_INVALID_ISPS_LEVEL"), "Expected SID_INVALID_ISPS_LEVEL but got: $codes")
    }

    @ParameterizedTest
    @ValueSource(ints = [2, 3])
    fun `shouldFailWithSidDesignatedAuthorityRequiredWhenIspsLevel2Or3AndNoAuthority`(level: Int) {
        val payload = validSidPayload(ispsLevel = level, designatedAuthority = null)
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.SID, payload, validImoNumber, validPortLocode)
        }
        val codes = exception.errors.map { it.code }
        assertTrue(codes.contains("SID_DESIGNATED_AUTHORITY_REQUIRED"), "Expected SID_DESIGNATED_AUTHORITY_REQUIRED but got: $codes")
    }

    @ParameterizedTest
    @ValueSource(ints = [2, 3])
    fun `shouldPassWhenSidIspsLevel2Or3AndDesignatedAuthorityIsProvided`(level: Int) {
        val payload = validSidPayload(ispsLevel = level, designatedAuthority = "Port Security Authority Rotterdam")
        assertDoesNotThrow {
            validator.validate(FormalityType.SID, payload, validImoNumber, validPortLocode)
        }
    }

    @Test
    fun `shouldFailWithSidDesignatedAuthorityRequiredWhenIspsLevel2AndAuthorityIsBlank`() {
        val payload = validSidPayload(ispsLevel = 2, designatedAuthority = "  ")
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.SID, payload, validImoNumber, validPortLocode)
        }
        val codes = exception.errors.map { it.code }
        assertTrue(codes.contains("SID_DESIGNATED_AUTHORITY_REQUIRED"))
    }

    // ===== Fail-all strategie: meerdere fouten =====

    @Test
    fun `shouldCollectMultipleErrorsWhenMultipleValidationRulesAreBroken`() {
        // Ongeldige LOCODE + ongeldige IMO + ongeldige MMSI
        val payload = validVidPayload().copy(mmsi = "12345")
        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.VID, payload, "INVALID_IMO", "invalid")
        }
        // Verwacht: INVALID_LOCODE + INVALID_IMO + VID_INVALID_MMSI
        assertTrue(exception.errors.size >= 3, "Verwacht >= 3 fouten maar kreeg: ${exception.errors.size}")
        val codes = exception.errors.map { it.code }
        assertTrue(codes.contains("INVALID_LOCODE"))
        assertTrue(codes.contains("INVALID_IMO"))
        assertTrue(codes.contains("VID_INVALID_MMSI"))
    }

    @Test
    fun `shouldPassCompleteValidNoaFormality`() {
        val payload = FormalityPayload.NoaPayload(
            expectedArrival = OffsetDateTime.now(ZoneOffset.UTC).plusHours(12),
            lastPortLocode = "GBFXT",
            nextPortLocode = "NLRTM",
            purposeOfCall = "Lossing containers",
            personsOnBoard = 22,
            dangerousGoods = false,
            wasteDelivery = true,
            maxStaticDraught = BigDecimal("11.50")
        )
        assertDoesNotThrow {
            validator.validate(FormalityType.NOA, payload, validImoNumber, validPortLocode)
        }
    }
}
