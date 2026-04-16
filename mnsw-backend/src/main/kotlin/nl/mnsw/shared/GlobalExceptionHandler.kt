package nl.mnsw.shared

import nl.mnsw.shared.exception.ConcurrentCorrectionException
import nl.mnsw.shared.exception.FormalityNotFoundException
import nl.mnsw.shared.exception.UnauthorizedAccessException
import nl.mnsw.shared.exception.ValidationException
import nl.mnsw.shared.exception.VisitNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Globale exception handler voor alle REST controllers.
 * Vertaalt domeinuitzonderingen naar gestructureerde HTTP-responses conform api.md.
 *
 * Foutstructuur conform EMSWe foutmodel:
 * { "errors": [{ "field": "...", "code": "...", "message": "..." }] }
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Domeinvalidatiefouten (business rule violations).
     * HTTP 422 Unprocessable Entity — conform api.md.
     */
    @ExceptionHandler(ValidationException::class)
    fun handleValidation(e: ValidationException): ResponseEntity<ErrorResponse> {
        val errors = e.errors.map { fieldError ->
            ErrorDetail(
                field = fieldError.field,
                code = fieldError.code,
                message = fieldError.message
            )
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ErrorResponse(errors = errors))
    }

    /**
     * Bean Validation fouten (@Valid/@Validated annotaties).
     * HTTP 400 Bad Request — ontbrekende of malformed velden.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleBeanValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.fieldErrors.map { fieldError ->
            ErrorDetail(
                field = fieldError.field,
                code = fieldError.code ?: "INVALID",
                message = fieldError.defaultMessage ?: "Ongeldig veld: ${fieldError.field}"
            )
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(errors = errors))
    }

    /**
     * Formality of Visit niet gevonden.
     * HTTP 404 Not Found.
     */
    @ExceptionHandler(FormalityNotFoundException::class, VisitNotFoundException::class)
    fun handleNotFound(e: RuntimeException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(errors = listOf(
                ErrorDetail(field = null, code = "NOT_FOUND", message = e.message ?: "Resource niet gevonden")
            )))
    }

    /**
     * Toegang geweigerd — ingelogd maar geen rechten.
     * HTTP 403 Forbidden.
     */
    @ExceptionHandler(UnauthorizedAccessException::class)
    fun handleUnauthorized(e: UnauthorizedAccessException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(errors = listOf(
                ErrorDetail(field = null, code = "FORBIDDEN", message = e.message ?: "Toegang geweigerd")
            )))
    }

    /**
     * Concurrent correctie conflict.
     * HTTP 409 Conflict — formality heeft al een nieuwere versie.
     */
    @ExceptionHandler(ConcurrentCorrectionException::class)
    fun handleConflict(e: ConcurrentCorrectionException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(errors = listOf(
                ErrorDetail(
                    field = null,
                    code = "CONCURRENT_CORRECTION",
                    message = "Formality ${e.formalityId} heeft al een nieuwere versie"
                )
            )))
    }

    /**
     * Onverwachte systeemfouten.
     * HTTP 500 Internal Server Error — generieke melding, interne details worden gelogd.
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Onverwachte fout", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(errors = listOf(
                ErrorDetail(field = null, code = "INTERNAL_ERROR", message = "Er is een onverwachte fout opgetreden")
            )))
    }
}

data class ErrorResponse(
    val errors: List<ErrorDetail>
)

data class ErrorDetail(
    val field: String?,
    val code: String,
    val message: String
)
