package nl.mnsw.formality.application

import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.FormalityValidator
import nl.mnsw.formality.domain.payload.FormalityPayload
import nl.mnsw.shared.exception.ValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.OffsetDateTime

class FormalityValidatorTest {

    private val validator = FormalityValidator()

    private val validImo = "9123453"  // Valid IMO: 9*7+1*6+2*5+3*4+4*3+5*2=113, check digit=3
    private val validLocode = "NLRTM"

    // ===== NOA Tests =====

    @Test
    fun `should pass validation for valid NOA payload`() {
        val payload = FormalityPayload.NoaPayload(
            expectedArrival = OffsetDateTime.now().plusDays(2),
            lastPortLocode = "GBFXT",
            nextPortLocode = "NLRTM",
            purposeOfCall = "Lossing",
            personsOnBoard = 22,
            dangerousGoods = false,
            wasteDelivery = true,
            maxStaticDraught = BigDecimal("11.5")
        )

        // Should not throw
        validator.validate(FormalityType.NOA, payload, validImo, validLocode)
    }

    @Test
    fun `should fail validation when NOA expectedArrival is more than 24 hours in past`() {
        val payload = FormalityPayload.NoaPayload(
            expectedArrival = OffsetDateTime.now().minusHours(25),
            lastPortLocode = null,
            nextPortLocode = null,
            purposeOfCall = null,
            personsOnBoard = null,
            dangerousGoods = false,
            wasteDelivery = false,
            maxStaticDraught = null
        )

        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOA, payload, validImo, validLocode)
        }

        assertTrue(exception.errors.any { it.field == "payload.expectedArrival" })
    }

    @Test
    fun `should fail validation when NOA personsOnBoard is negative`() {
        val payload = FormalityPayload.NoaPayload(
            expectedArrival = OffsetDateTime.now().plusDays(1),
            lastPortLocode = null,
            nextPortLocode = null,
            purposeOfCall = null,
            personsOnBoard = -1,
            dangerousGoods = false,
            wasteDelivery = false,
            maxStaticDraught = null
        )

        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOA, payload, validImo, validLocode)
        }

        assertTrue(exception.errors.any { it.field == "payload.personsOnBoard" })
    }

    @Test
    fun `should fail validation when NOA maxStaticDraught is 50 or more`() {
        val payload = FormalityPayload.NoaPayload(
            expectedArrival = OffsetDateTime.now().plusDays(1),
            lastPortLocode = null,
            nextPortLocode = null,
            purposeOfCall = null,
            personsOnBoard = null,
            dangerousGoods = false,
            wasteDelivery = false,
            maxStaticDraught = BigDecimal("50.00")
        )

        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOA, payload, validImo, validLocode)
        }

        assertTrue(exception.errors.any { it.field == "payload.maxStaticDraught" })
    }

    // ===== NOS Tests =====

    @Test
    fun `should fail validation when NOS actualSailing is more than 48 hours in future`() {
        val payload = FormalityPayload.NosPayload(
            actualSailing = OffsetDateTime.now().plusHours(49),
            nextPortLocode = null,
            destinationCountry = null
        )

        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOS, payload, validImo, validLocode)
        }

        assertTrue(exception.errors.any { it.field == "payload.actualSailing" })
    }

    @Test
    fun `should pass validation for valid NOS payload within 48 hours`() {
        val payload = FormalityPayload.NosPayload(
            actualSailing = OffsetDateTime.now().plusHours(24),
            nextPortLocode = "DEHAM",
            destinationCountry = "DE"
        )

        validator.validate(FormalityType.NOS, payload, validImo, validLocode)
    }

    // ===== NOD Tests =====

    @Test
    fun `should fail validation when NOD expectedDeparture is in the past`() {
        val payload = FormalityPayload.NodPayload(
            expectedDeparture = OffsetDateTime.now().minusHours(1),
            nextPortLocode = null,
            destinationCountry = null,
            lastCargoOperations = null
        )

        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOD, payload, validImo, validLocode)
        }

        assertTrue(exception.errors.any { it.field == "payload.expectedDeparture" })
    }

    // ===== VID Tests =====

    @Test
    fun `should fail validation when VID mmsi has wrong format`() {
        val payload = FormalityPayload.VidPayload(
            certificateNationality = null,
            grossTonnage = null,
            netTonnage = null,
            deadweight = null,
            lengthOverall = null,
            shipType = null,
            callSign = null,
            mmsi = "12345"  // Te kort — moet 9 cijfers zijn
        )

        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.VID, payload, validImo, validLocode)
        }

        assertTrue(exception.errors.any { it.field == "payload.mmsi" })
    }

    @Test
    fun `should fail validation when VID callSign has wrong format`() {
        val payload = FormalityPayload.VidPayload(
            certificateNationality = null,
            grossTonnage = null,
            netTonnage = null,
            deadweight = null,
            lengthOverall = null,
            shipType = null,
            callSign = "AB",  // Te kort — minimaal 3 tekens
            mmsi = null
        )

        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.VID, payload, validImo, validLocode)
        }

        assertTrue(exception.errors.any { it.field == "payload.callSign" })
    }

    // ===== SID Tests =====

    @Test
    fun `should fail validation when SID ispsLevel is not 1, 2, or 3`() {
        val payload = FormalityPayload.SidPayload(
            ispsLevel = 4,
            last10Ports = emptyList(),
            securityDeclaration = null,
            shipToShipActivities = false,
            designatedAuthority = null,
            ssasActivated = false
        )

        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.SID, payload, validImo, validLocode)
        }

        assertTrue(exception.errors.any { it.field == "payload.ispsLevel" })
    }

    @Test
    fun `should fail validation when SID ispsLevel is 2 but designatedAuthority is missing`() {
        val payload = FormalityPayload.SidPayload(
            ispsLevel = 2,
            last10Ports = emptyList(),
            securityDeclaration = null,
            shipToShipActivities = false,
            designatedAuthority = null,  // Verplicht bij level >= 2
            ssasActivated = false
        )

        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.SID, payload, validImo, validLocode)
        }

        assertTrue(exception.errors.any { it.field == "payload.designatedAuthority" })
    }

    @Test
    fun `should pass validation when SID ispsLevel is 2 with designatedAuthority`() {
        val payload = FormalityPayload.SidPayload(
            ispsLevel = 2,
            last10Ports = emptyList(),
            securityDeclaration = "STATEMENT",
            shipToShipActivities = false,
            designatedAuthority = "Rijkswaterstaat",
            ssasActivated = false
        )

        validator.validate(FormalityType.SID, payload, validImo, validLocode)
    }

    @Test
    fun `should collect multiple errors in single ValidationException`() {
        val payload = FormalityPayload.NoaPayload(
            expectedArrival = OffsetDateTime.now().minusHours(25),  // Te vroeg
            lastPortLocode = null,
            nextPortLocode = null,
            purposeOfCall = null,
            personsOnBoard = -5,  // Negatief
            dangerousGoods = false,
            wasteDelivery = false,
            maxStaticDraught = BigDecimal("100.0")  // Te groot
        )

        val exception = assertThrows<ValidationException> {
            validator.validate(FormalityType.NOA, payload, validImo, validLocode)
        }

        assertTrue(exception.errors.size >= 2)
    }
}
