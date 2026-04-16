package nl.mnsw.shared.exception

import java.util.UUID

/**
 * Gegooid wanneer een visit niet gevonden wordt.
 * HTTP 404 Not Found via GlobalExceptionHandler.
 */
class VisitNotFoundException(
    val visitId: UUID
) : RuntimeException("Visit not found: $visitId")
