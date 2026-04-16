package nl.mnsw.support

import nl.mnsw.auth.domain.Role
import nl.mnsw.auth.infrastructure.MnswUserDetails
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory
import java.util.UUID

/**
 * Test-annotatie om een MNSW-gebruiker in de Spring Security context te plaatsen.
 * Ondersteunt userId, rol en portLocode — de drie relevante velden voor MNSW autorisatie.
 *
 * Gebruik:
 *   @WithMnswUser(role = "SCHEEPSAGENT")
 *   @WithMnswUser(role = "HAVENAUTORITEIT", portLocode = "NLRTM")
 *   @WithMnswUser(role = "ADMIN")
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = WithMnswUserSecurityContextFactory::class)
annotation class WithMnswUser(
    val userId: String = "00000000-0000-0000-0000-000000000001",
    val email: String = "test@mnsw.nl",
    val fullName: String = "Test Gebruiker",
    val role: String = "SCHEEPSAGENT",
    val portLocode: String = ""
)

class WithMnswUserSecurityContextFactory : WithSecurityContextFactory<WithMnswUser> {

    override fun createSecurityContext(annotation: WithMnswUser): SecurityContext {
        val role = Role.valueOf(annotation.role)
        val portLocode = annotation.portLocode.takeIf { it.isNotBlank() }

        val userDetails = MnswUserDetails(
            userId = UUID.fromString(annotation.userId),
            email = annotation.email,
            passwordHash = "\$2b\$12\$testHash",
            role = role,
            portLocode = portLocode,
            active = true
        )

        val authentication = UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.authorities
        )

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        return context
    }
}
