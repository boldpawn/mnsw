package nl.mnsw.auth.infrastructure.web

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import nl.mnsw.auth.application.AuthenticateCommand
import nl.mnsw.auth.application.AuthenticateUseCase
import nl.mnsw.auth.application.InvalidCredentialsException
import nl.mnsw.auth.domain.Role
import nl.mnsw.shared.ErrorDetail
import nl.mnsw.shared.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

/**
 * REST controller voor authenticatie.
 * POST /api/v1/auth/login — JWT-token ophalen met e-mail en wachtwoord.
 *
 * Endpoint is publiek toegankelijk (zie SecurityConfig).
 * Bij ongeldige credentials: HTTP 401 (bewust vaag om gebruikersnamen niet te lekken).
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authenticateUseCase: AuthenticateUseCase
) {

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: LoginRequest
    ): ResponseEntity<*> {
        return try {
            val result = authenticateUseCase.execute(
                AuthenticateCommand(
                    email = request.email,
                    password = request.password
                )
            )

            ResponseEntity.ok(
                LoginResponse(
                    token = result.token,
                    expiresAt = result.expiresAt,
                    user = UserInfoDto(
                        id = result.user.id,
                        email = result.user.email,
                        fullName = result.user.fullName,
                        role = result.user.role
                    )
                )
            )
        } catch (e: InvalidCredentialsException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse(errors = listOf(
                    ErrorDetail(field = null, code = "INVALID_CREDENTIALS", message = "Ongeldige gebruikersnaam of wachtwoord")
                )))
        }
    }
}

data class LoginRequest(
    @field:NotBlank(message = "Email is verplicht")
    @field:Email(message = "Geen geldig e-mailadres")
    val email: String = "",

    @field:NotBlank(message = "Wachtwoord is verplicht")
    val password: String = ""
)

data class LoginResponse(
    val token: String,
    val expiresAt: OffsetDateTime,
    val user: UserInfoDto
)

data class UserInfoDto(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: Role
)
