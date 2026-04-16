package nl.mnsw.shared.exception

/**
 * Domeinvalidatiefout. Bevat een lijst van veldfouten die aan de indiener worden teruggegeven.
 * HTTP 422 Unprocessable Entity via GlobalExceptionHandler.
 * Voor RIM-indieners: resulteert ook in een FRM-response met status REJECTED.
 */
class ValidationException(
    val errors: List<FieldError>
) : RuntimeException("Validation failed: ${errors.size} error(s)")

/**
 * Een individuele veldfout conform EMSWe foutstructuur.
 */
data class FieldError(
    val field: String,
    val code: String,
    val message: String
)
