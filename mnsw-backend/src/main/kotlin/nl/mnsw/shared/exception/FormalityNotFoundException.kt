package nl.mnsw.shared.exception

import java.util.UUID

/**
 * Gegooid wanneer een formality niet gevonden wordt.
 * HTTP 404 Not Found via GlobalExceptionHandler.
 */
class FormalityNotFoundException(
    val formalityId: UUID
) : RuntimeException("Formality not found: $formalityId")
