package nl.mnsw.auth.infrastructure.web

import com.fasterxml.jackson.databind.ObjectMapper
import nl.mnsw.auth.application.AuthenticateCommand
import nl.mnsw.auth.application.AuthenticateResult
import nl.mnsw.auth.application.AuthenticateUseCase
import nl.mnsw.auth.application.InvalidCredentialsException
import nl.mnsw.auth.domain.Role
import nl.mnsw.auth.domain.User
import nl.mnsw.auth.infrastructure.JwtService
import nl.mnsw.auth.infrastructure.MnswUserDetailsService
import nl.mnsw.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Integratietests voor AuthController.
 * Importeert SecurityConfig voor echte beveiligingsconfiguratie.
 * JwtService en MnswUserDetailsService worden gemocked zodat de JwtAuthenticationFilter
 * geen echte JWT-validatie uitvoert, maar de filterchain wel doorgeeft.
 */
@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var authenticateUseCase: AuthenticateUseCase

    @MockitoBean
    private lateinit var jwtService: JwtService

    @MockitoBean
    private lateinit var userDetailsService: MnswUserDetailsService

    private val userId = UUID.randomUUID()
    private val testUser = User(
        id = userId,
        email = "agent@rederij.nl",
        passwordHash = "\$2b\$12\$hash",
        fullName = "Jan Jansen",
        eori = null,
        role = Role.SCHEEPSAGENT,
        portLocode = null,
        active = true
    )

    @Test
    fun `POST login should return 200 with token on valid credentials`() {
        val expiresAt = OffsetDateTime.now().plusHours(8)
        whenever(authenticateUseCase.execute(any<AuthenticateCommand>())).thenReturn(
            AuthenticateResult(
                token = "eyJ.test.token",
                expiresAt = expiresAt,
                user = testUser
            )
        )

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("email" to "agent@rederij.nl", "password" to "geheim")
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { value("eyJ.test.token") }
            jsonPath("$.user.email") { value("agent@rederij.nl") }
            jsonPath("$.user.role") { value("SCHEEPSAGENT") }
        }
    }

    @Test
    fun `POST login should return 401 on invalid credentials`() {
        whenever(authenticateUseCase.execute(any<AuthenticateCommand>()))
            .thenThrow(InvalidCredentialsException())

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("email" to "agent@rederij.nl", "password" to "wrongpassword")
            )
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.errors[0].code") { value("INVALID_CREDENTIALS") }
        }
    }

    @Test
    fun `POST login should return 401 on unknown email`() {
        whenever(authenticateUseCase.execute(any<AuthenticateCommand>()))
            .thenThrow(InvalidCredentialsException())

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("email" to "unknown@example.com", "password" to "wachtwoord")
            )
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.errors") { isArray() }
        }
    }

    @Test
    fun `POST login should return 400 when email is blank`() {
        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("email" to "", "password" to "geheim")
            )
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST login should return 400 when password is blank`() {
        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("email" to "agent@rederij.nl", "password" to "")
            )
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
