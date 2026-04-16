package nl.mnsw.shared.exception

/**
 * Gegooid wanneer een ingelogde gebruiker geen toegang heeft tot de gevraagde resource.
 * Bijvoorbeeld: scheepsagent probeert formality van een andere agent te corrigeren.
 * HTTP 403 Forbidden via GlobalExceptionHandler.
 */
class UnauthorizedAccessException(
    message: String
) : RuntimeException(message)
