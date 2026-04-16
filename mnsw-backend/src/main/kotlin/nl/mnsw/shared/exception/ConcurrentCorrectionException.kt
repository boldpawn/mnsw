package nl.mnsw.shared.exception

import java.util.UUID

/**
 * Gegooid wanneer een correctie wordt ingediend op een formality die al een nieuwere versie heeft.
 * Beschermt tegen race conditions bij gelijktijdige correcties.
 * HTTP 409 Conflict via GlobalExceptionHandler.
 */
class ConcurrentCorrectionException(
    val formalityId: UUID
) : RuntimeException("Formality $formalityId already has a newer version — concurrent correction detected")
