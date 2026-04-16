package nl.mnsw.auth.application

import nl.mnsw.auth.domain.Role
import nl.mnsw.auth.domain.User
import nl.mnsw.auth.infrastructure.JwtService
import nl.mnsw.auth.infrastructure.persistence.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Use case voor het authenticeren van een gebruiker via e-mail en wachtwoord.
 * Geeft een JWT-token terug bij succesvolle authenticatie.
 *
 * Gooit InvalidCredentialsException bij ongeldige credentials of inactieve gebruiker.
 */
@Service
@Transactional(readOnly = true)
class AuthenticateUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    fun execute(command: AuthenticateCommand): AuthenticateResult {
        // 1. Zoek gebruiker op e-mailadres
        val userEntity = userRepository.findByEmail(command.email)
            ?: throw InvalidCredentialsException()

        // 2. Controleer of account actief is
        if (!userEntity.active) {
            throw InvalidCredentialsException()
        }

        // 3. Valideer wachtwoord
        if (!passwordEncoder.matches(command.password, userEntity.passwordHash)) {
            throw InvalidCredentialsException()
        }

        // 4. Maak domein-User aan
        val user = User(
            id = userEntity.id,
            email = userEntity.email,
            passwordHash = userEntity.passwordHash,
            fullName = userEntity.fullName,
            eori = userEntity.eori,
            role = userEntity.role,
            portLocode = userEntity.portLocode,
            active = userEntity.active
        )

        // 5. Genereer JWT-token
        val token = jwtService.generateToken(user)
        val expiresAt = jwtService.extractExpiration(token)

        return AuthenticateResult(
            token = token,
            expiresAt = expiresAt,
            user = user
        )
    }
}

data class AuthenticateCommand(
    val email: String,
    val password: String
)

data class AuthenticateResult(
    val token: String,
    val expiresAt: OffsetDateTime,
    val user: User
)

/**
 * Gegooid wanneer de opgegeven credentials ongeldig zijn.
 * HTTP 401 Unauthorized — bewust vaag om geen informatie te lekken over bestaan gebruiker.
 */
class InvalidCredentialsException : RuntimeException("Ongeldige credentials")
