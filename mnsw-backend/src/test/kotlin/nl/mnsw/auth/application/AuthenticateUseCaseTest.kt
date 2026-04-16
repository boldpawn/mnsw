package nl.mnsw.auth.application

import nl.mnsw.auth.domain.Role
import nl.mnsw.auth.infrastructure.JwtService
import nl.mnsw.auth.infrastructure.persistence.UserJpaEntity
import nl.mnsw.auth.infrastructure.persistence.UserRepository
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.OffsetDateTime
import java.util.UUID

class AuthenticateUseCaseTest {

    private val userRepository: UserRepository = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private val jwtService: JwtService = mock()

    private val useCase = AuthenticateUseCase(
        userRepository = userRepository,
        passwordEncoder = passwordEncoder,
        jwtService = jwtService
    )

    private val userId = UUID.randomUUID()
    private val userEntity = UserJpaEntity(
        id = userId,
        email = "agent@rederij.nl",
        passwordHash = "\$2b\$12\$hashedPassword",
        fullName = "Jan Jansen",
        role = Role.SCHEEPSAGENT,
        portLocode = null,
        active = true
    )

    @Test
    fun `should return token when credentials are valid`() {
        whenever(userRepository.findByEmail("agent@rederij.nl")).thenReturn(userEntity)
        whenever(passwordEncoder.matches("geheim", userEntity.passwordHash)).thenReturn(true)
        whenever(jwtService.generateToken(any())).thenReturn("eyJ.test.token")
        whenever(jwtService.extractExpiration("eyJ.test.token")).thenReturn(OffsetDateTime.now().plusHours(8))

        val command = AuthenticateCommand(email = "agent@rederij.nl", password = "geheim")
        val result = useCase.execute(command)

        assertEquals("eyJ.test.token", result.token)
        assertNotNull(result.expiresAt)
        assertEquals("agent@rederij.nl", result.user.email)
        assertEquals(Role.SCHEEPSAGENT, result.user.role)
    }

    @Test
    fun `should throw InvalidCredentialsException when user does not exist`() {
        whenever(userRepository.findByEmail("unknown@test.nl")).thenReturn(null)

        assertThrows<InvalidCredentialsException> {
            useCase.execute(AuthenticateCommand("unknown@test.nl", "wachtwoord"))
        }
    }

    @Test
    fun `should throw InvalidCredentialsException when password is wrong`() {
        whenever(userRepository.findByEmail("agent@rederij.nl")).thenReturn(userEntity)
        whenever(passwordEncoder.matches("wrongpassword", userEntity.passwordHash)).thenReturn(false)

        assertThrows<InvalidCredentialsException> {
            useCase.execute(AuthenticateCommand("agent@rederij.nl", "wrongpassword"))
        }
    }

    @Test
    fun `should throw InvalidCredentialsException when user is inactive`() {
        val inactiveUser = UserJpaEntity(
            id = userId,
            email = "agent@rederij.nl",
            passwordHash = "\$2b\$12\$hashedPassword",
            fullName = "Jan Jansen",
            role = Role.SCHEEPSAGENT,
            active = false
        )
        whenever(userRepository.findByEmail("agent@rederij.nl")).thenReturn(inactiveUser)

        assertThrows<InvalidCredentialsException> {
            useCase.execute(AuthenticateCommand("agent@rederij.nl", "geheim"))
        }
    }

    @Test
    fun `should map user domain object correctly from entity`() {
        whenever(userRepository.findByEmail("agent@rederij.nl")).thenReturn(userEntity)
        whenever(passwordEncoder.matches("geheim", userEntity.passwordHash)).thenReturn(true)
        whenever(jwtService.generateToken(any())).thenReturn("token")
        whenever(jwtService.extractExpiration("token")).thenReturn(OffsetDateTime.now().plusHours(8))

        val result = useCase.execute(AuthenticateCommand("agent@rederij.nl", "geheim"))

        assertEquals(userId, result.user.id)
        assertEquals("Jan Jansen", result.user.fullName)
        assertEquals(true, result.user.active)
    }
}
